package com.kodeco.recipefinder.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kodeco.recipefinder.data.Prefs
import com.kodeco.recipefinder.data.Repository
import com.kodeco.recipefinder.data.ingredientDbsToIngredients
import com.kodeco.recipefinder.data.ingredientDbsToSpoonacular
import com.kodeco.recipefinder.data.models.Ingredient
import com.kodeco.recipefinder.data.models.Recipe
import com.kodeco.recipefinder.data.recipeDbsToRecipes
import com.kodeco.recipefinder.data.recipeToDb
import com.kodeco.recipefinder.data.recipeToSpoonacularRecipe
import com.kodeco.recipefinder.data.spoonacularIngredientsToIngredients
import com.kodeco.recipefinder.data.spoonacularRecipeToRecipe
import com.kodeco.recipefinder.network.RetrofitInstance
import com.kodeco.recipefinder.network.SpoonacularRecipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

data class QueryState(
    val query: String = "",
    val offset: Int = 0,
    val number: Int = 20,
    val totalResults: Int = 0,
)

data class UIState(
    val searching: Boolean = false,
    val allChecked: Boolean = true,
    val bookmarksChecked: Boolean = false,
    val previousSearches: MutableList<String> = arrayListOf()
)

const val PAGE_SIZE = 20

class RecipeViewModel(private val prefs: Prefs) : ViewModel() {
    companion object {
        const val PREVIOUS_SEARCH_KEY = "PREVIOUS_SEARCH_KEY"
    }

    private val spoonacularService = RetrofitInstance.spoonacularService

    private val _recipeListState = MutableStateFlow<List<Recipe>>(listOf())
    val recipeListState = _recipeListState.asStateFlow()

    private val _recipeState = MutableStateFlow<SpoonacularRecipe?>(null)
    val recipeState = _recipeState.asStateFlow()

    private val _queryState = MutableStateFlow(QueryState())
    val queryState = _queryState.asStateFlow()

    private val _uiState = MutableStateFlow(UIState())
    val uiState = _uiState.asStateFlow()

    private val _bookmarksState = MutableStateFlow<MutableList<Recipe>>(mutableListOf())
    val bookmarksState = _bookmarksState.asStateFlow()

    private val _ingredientsState = MutableStateFlow<MutableList<Ingredient>>(mutableListOf())
    val ingredientsState = _ingredientsState.asStateFlow()

    init {
        viewModelScope.launch {
            val previousSearchString = prefs.getString(PREVIOUS_SEARCH_KEY)
            if (!previousSearchString.isNullOrEmpty()) {
                val currentList = previousSearchString.split(",")
                val updatedList = mutableListOf<String>()
                updatedList.addAll(currentList)
                _uiState.value = _uiState.value.copy(previousSearches = updatedList)
            }
        }
    }

    fun queryRecipies(
        query: String,
        offset: Int,
        number: Int = PAGE_SIZE
    ) {
        viewModelScope.launch {
            try {
                val response = spoonacularService.queryRecipes(query, offset, number)
                _recipeListState.value = _recipeListState.value.plus(response.recipes)
                _queryState.value =
                    QueryState(query, offset, number, response.totalResults)
            } catch (e: Exception) {
                Timber.e(e, "Problems getting Recipes")
                _recipeListState.value = listOf()
            }
        }
    }

    private fun savePreviousSearches() {
        viewModelScope.launch {
            val searchString = _uiState.value.previousSearches.joinToString(",")
            prefs.saveString(PREVIOUS_SEARCH_KEY, searchString)
        }
    }

    suspend fun queryRecipe(
        id: Int,
    ) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val spoonacularRecipe = spoonacularService.queryRecipe(id)
                    _recipeState.value = spoonacularRecipe
                }
            } catch (e: Exception) {
                Timber.e(e, "Problems getting Recipe for id $id")
                _recipeState.value = null
            }
        }
    }

    suspend fun getBookmarks(repository: Repository) {
        withContext(Dispatchers.IO) {
            val allRecipes = repository.findAllRecipes()
            _bookmarksState.value = recipeDbsToRecipes(allRecipes).toMutableList()
        }
    }

    suspend fun getIngredients(repository: Repository) {
        withContext(Dispatchers.IO) {
            val allIngredients = repository.findAllIngredients()
            _ingredientsState.value = ingredientDbsToIngredients(allIngredients).toMutableList()
        }
    }

    suspend fun getBookmark(repository: Repository, bookmarkId: Int) {
        withContext(Dispatchers.IO) {
            val recipe = repository.findRecipeById(bookmarkId)
            val ingredients = repository.findRecipeIngredients(bookmarkId)
            _recipeState.value =
                recipeToSpoonacularRecipe(recipe, ingredientDbsToSpoonacular(ingredients))
        }
    }

    suspend fun bookmarkRecipe(repository: Repository, recipe: SpoonacularRecipe) {
        withContext(Dispatchers.IO) {
            repository.insertRecipe(spoonacularRecipeToRecipe(recipe))
            repository.insertIngredients(
                spoonacularIngredientsToIngredients(
                    recipe.id,
                    recipe.extendedIngredients
                )
            )
        }
    }

    fun addPreviousSearch(searchString: String) {
        if (!_uiState.value.previousSearches.contains(searchString)) {
            val updatedSearches = mutableListOf<String>()
            updatedSearches.addAll(uiState.value.previousSearches)
            updatedSearches.add(searchString)
            _uiState.value = _uiState.value.copy(previousSearches = updatedSearches)
            savePreviousSearches()
        }
    }

    fun updateQueryState(queryState: QueryState) {
        _queryState.value = queryState
    }

    fun setSearching(searching: Boolean = true) {
        _uiState.value = _uiState.value.copy(searching = searching)
    }

    fun setAllChecked(allChecked: Boolean = true) {
        _uiState.value = _uiState.value.copy(allChecked = allChecked)
    }

    fun setBookmarksChecked(bookmarksChecked: Boolean = true) {
        _uiState.value = _uiState.value.copy(bookmarksChecked = bookmarksChecked)
    }

    suspend fun deleteBookmark(repository: Repository, recipe: Recipe) {
        withContext(Dispatchers.IO) {
            val recipeDb = recipeToDb(recipe)
            repository.deleteRecipe(recipeDb)
            val localList = _bookmarksState.value.toMutableList()
            localList.remove(recipe)
            _bookmarksState.value = localList
        }
    }

    suspend fun deleteBookmark(repository: Repository, recipeId: Int) {
        withContext(Dispatchers.IO) {
            repository.deleteRecipeById(recipeId.toLong())
            val localList = _bookmarksState.value.toMutableList()
            localList.removeIf {
                it.id == recipeId
            }
            _bookmarksState.value = localList
        }
    }
}