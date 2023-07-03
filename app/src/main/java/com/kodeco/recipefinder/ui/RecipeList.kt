package com.kodeco.recipefinder.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DismissDirection
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismiss
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDismissState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.kodeco.recipefinder.LocalNavigatorProvider
import com.kodeco.recipefinder.LocalPrefsProvider
import com.kodeco.recipefinder.LocalRepositoryProvider
import com.kodeco.recipefinder.R
import com.kodeco.recipefinder.data.models.Recipe
import com.kodeco.recipefinder.ui.theme.LabelLarge
import com.kodeco.recipefinder.ui.theme.lightGreen
import com.kodeco.recipefinder.ui.theme.lightGrey
import com.kodeco.recipefinder.ui.theme.transparent
import com.kodeco.recipefinder.ui.widgets.BookmarkCard
import com.kodeco.recipefinder.ui.widgets.RecipeCard
import com.kodeco.recipefinder.ui.widgets.SpacerMax
import com.kodeco.recipefinder.ui.widgets.SpacerW4
import com.kodeco.recipefinder.utils.viewModelFactory
import com.kodeco.recipefinder.viewmodels.PAGE_SIZE
import com.kodeco.recipefinder.viewmodels.RecipeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val PAGING_OFFSET = 6

@Composable
fun RecipeList() {
    val prefs = LocalPrefsProvider.current
    val viewModel: RecipeViewModel = viewModel(factory = viewModelFactory {
        RecipeViewModel(prefs)
    })
    val recipeListState = remember { mutableStateOf(listOf<Recipe>()) }
    val scope = rememberCoroutineScope()
    val navController = LocalNavigatorProvider.current
    val uiState = viewModel.uiState.collectAsState()
    LaunchedEffect(Unit) {
        scope.launch {
            viewModel.queryState.collect { state ->
                if (state.number > 0) {
                    viewModel.setSearching(false)
                }
            }
        }
        scope.launch {
            viewModel.recipeListState.collect { state ->
                recipeListState.value = state
            }
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        ImageRow()
        ChipRow(viewModel)
        if (uiState.value.allChecked) {
            SearchRow(viewModel)
        }
        if (uiState.value.allChecked) {
            ShowRecipeList(recipeListState, viewModel, navController)
        } else {
            ShowBookmarks(viewModel)
        }
    }
}

@Composable
fun ShowBookmarks(viewModel: RecipeViewModel) {
    val scope = rememberCoroutineScope()
    val repository = LocalRepositoryProvider.current
    val bookmarkListState = remember { mutableStateOf(listOf<Recipe>()) }
    LaunchedEffect(Unit) {
        scope.launch {
            viewModel.bookmarksState.collect { bookmarks ->
                bookmarkListState.value = bookmarks
            }
        }
        scope.launch {
            withContext(Dispatchers.IO) {
                viewModel.getBookmarks(repository)
            }
        }
    }
    val bookmarks = bookmarkListState.value
    LazyColumn(content = {
        items(
            count = bookmarks.size,
            key = { index -> bookmarks[index].id },
            itemContent = { index ->
                val recipe = bookmarks[index]
                val currentItem by rememberUpdatedState(recipe)
                val dismissState = rememberDismissState(
                    confirmValueChange = {
                        scope.launch {
                            withContext(Dispatchers.IO) {
                                viewModel.deleteBookmark(repository, currentItem)
                            }
                        }
                        true
                    }
                )
                SwipeToDismiss(state = dismissState, background = {
                    val alignment = when (dismissState.dismissDirection) {
                        DismissDirection.StartToEnd -> Alignment.CenterStart
                        DismissDirection.EndToStart -> Alignment.CenterEnd
                        null -> return@SwipeToDismiss
                    }
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(lightGrey)
                            .padding(horizontal = 20.dp),
                        contentAlignment = alignment
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                        )
                    }
                }, dismissContent = {
                    BookmarkCard(modifier = Modifier.fillMaxWidth(), recipe)
                })
            }
        )
    })
}

@Composable
private fun ColumnScope.ShowRecipeList(
    recipes: MutableState<List<Recipe>>,
    viewModel: RecipeViewModel,
    navController: NavHostController
) {
    val queryState = viewModel.queryState.collectAsState()
    val uiState = viewModel.uiState.collectAsState()
    val lazyGridState: LazyGridState = rememberLazyGridState()
    val loadMore = remember(lazyGridState) {
        derivedStateOf {
            val lastVisibleIndex =
                lazyGridState.firstVisibleItemIndex + lazyGridState.layoutInfo.visibleItemsInfo.size
//            Timber.e("FirstVisible ${lazyGridState.firstVisibleItemIndex} lastVisibleIndex: $lastVisibleIndex")
            lastVisibleIndex + PAGING_OFFSET > recipes.value.size &&
                    lastVisibleIndex < queryState.value.totalResults
        }
    }
    val firstTime = remember {
        mutableStateOf(true)
    }
//    Timber.e("Num Recipes: ${recipes.value.size} Total Results ${queryState.value.totalResults}")
//    Timber.e("First Visible Index: ${lazyGridState.firstVisibleItemIndex}")
//    Timber.e("Load More: ${loadMore.value}")
    LaunchedEffect(loadMore.value) {
        val offset = if (firstTime.value) 0 else queryState.value.offset + PAGE_SIZE
        firstTime.value = false
//        Timber.e("New Offset $offset")
        viewModel.updateQueryState(queryState.value.copy(offset = offset))
//        Timber.e("Searching ${queryState.value.query} at offset ${queryState.value.offset}")
        searchRecipes(queryState.value.query, viewModel)
    }
    if (uiState.value.searching) {
        Progress()
    } else {
        LazyVerticalGrid(
            state = lazyGridState,
            modifier = Modifier.fillMaxSize(),
            columns = GridCells.Adaptive(124.dp),
            // content padding
            contentPadding = PaddingValues(
                start = 12.dp,
                top = 16.dp,
                end = 12.dp,
                bottom = 16.dp
            ),
            content = {
                itemsIndexed(recipes.value, key = { _, item ->
                    item.id
                }) { _, item ->
//                    Timber.e("Display item: ${item.title} at index: $index and image: ${item.image}")
                    RecipeCard(
                        modifier = Modifier.clickable {
                            navController.navigate("details/${item.id}")
                        },
                        item
                    )
                }
            }
        )
    }
}

@Composable
fun ImageRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(lightGreen)
    ) {
        Image(
            modifier = Modifier.align(Alignment.Center),
            painter = painterResource(id = R.drawable.background2),
            contentDescription = null
        )
    }
}

@Composable
fun SearchRow(
    viewModel: RecipeViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val searchText = remember {
        mutableStateOf("")
    }
    val uiState = viewModel.uiState.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Box(modifier = Modifier
            .align(Alignment.CenterVertically)
            .clickable {
                if (searchText.value.isNotEmpty()) {
                    viewModel.setSearching(true)
                    keyboard?.hide()
                    searchRecipes(searchText.value.trim(), viewModel)
                }
            }) {
            Icon(
                Icons.Filled.Search,
                contentDescription = "Search",
            )
        }
        SpacerW4()
        TextField(
            modifier = Modifier.fillMaxWidth(0.8f),
            value = searchText.value,
            onValueChange = {
                searchText.value = it
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.setSearching(true)
                    keyboard?.hide()
                    searchRecipes(searchText.value.trim(), viewModel)
                },
            ),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = transparent,
                unfocusedContainerColor = transparent,
                focusedIndicatorColor = transparent,
                unfocusedIndicatorColor = transparent,
            ),
            label = { Text("Search...") },
        )
        SpacerMax()
        Box(modifier = Modifier.align(Alignment.CenterVertically)) {
            IconButton(onClick = {
                searchText.value = ""
            }) {
                Icon(Icons.Filled.Clear, contentDescription = "Filter")
            }
        }
        Box {
            // 3 vertical dots icon
            IconButton(onClick = {
                expanded = true
            }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Open Options"
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                uiState.value.previousSearches.forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            expanded = false
                            searchText.value = it
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ColumnScope.ChipRow(
    viewModel: RecipeViewModel,
) {
    val uiState = viewModel.uiState.collectAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.CenterHorizontally),
        horizontalArrangement = Arrangement.Center
    ) {
        SpacerW4()
        FilterChip(selected = uiState.value.allChecked, leadingIcon = {
            if (uiState.value.allChecked) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
            }
        }, onClick = {
            val allChecked = !uiState.value.allChecked
            viewModel.setAllChecked(allChecked)
            if (allChecked) {
                viewModel.setBookmarksChecked(false)
            }
        }, label = { Text(text = "All", style = LabelLarge) })
        SpacerW4()
        FilterChip(selected = uiState.value.bookmarksChecked, leadingIcon = {
            if (uiState.value.bookmarksChecked) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null)
            }
        }, onClick = {
            val bookmarksChecked = !uiState.value.bookmarksChecked
            viewModel.setBookmarksChecked(bookmarksChecked)
            if (bookmarksChecked) {
                viewModel.setAllChecked(false)
            }
        }, label = { Text(text = "Bookmarks", style = LabelLarge) })
    }
}

@Composable
fun ColumnScope.Progress() {
    CircularProgressIndicator(modifier = Modifier.Companion.align(Alignment.CenterHorizontally))
}

fun searchRecipes(searchString: String, viewModel: RecipeViewModel) {
    if (searchString.isNotEmpty()) {
        viewModel.addPreviousSearch(searchString)
        viewModel.queryRecipies(searchString, viewModel.queryState.value.offset)
    } else {
        viewModel.setSearching(false)
    }
}

@Preview
@Composable
fun PreviewRecipeList() {
    Surface {
        RecipeList()
    }
}