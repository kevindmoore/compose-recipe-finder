package com.kodeco.recipefinder.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kodeco.recipefinder.LocalPrefsProvider
import com.kodeco.recipefinder.LocalRepositoryProvider
import com.kodeco.recipefinder.R
import com.kodeco.recipefinder.data.models.Ingredient
import com.kodeco.recipefinder.ui.theme.BodyLarge
import com.kodeco.recipefinder.ui.theme.GroceryTitle
import com.kodeco.recipefinder.ui.theme.background1Color
import com.kodeco.recipefinder.ui.theme.iconBackgroundColor
import com.kodeco.recipefinder.ui.theme.transparent
import com.kodeco.recipefinder.ui.widgets.SpacerMax
import com.kodeco.recipefinder.ui.widgets.SpacerW4
import com.kodeco.recipefinder.utils.rememberState
import com.kodeco.recipefinder.utils.viewModelFactory
import com.kodeco.recipefinder.viewmodels.GroceryListViewModel
import com.kodeco.recipefinder.viewmodels.RecipeViewModel
import kotlinx.coroutines.launch

@Composable
fun GroceryList() {
    val prefs = LocalPrefsProvider.current
    val recipeViewModel: RecipeViewModel = viewModel(factory = viewModelFactory {
        RecipeViewModel(prefs)
    })
    val groceryListViewModel: GroceryListViewModel = viewModel()
    val repository = LocalRepositoryProvider.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            recipeViewModel.ingredientsState.collect { ingredients ->
                groceryListViewModel.setIngredients(ingredients)
            }
        }
        scope.launch {
            recipeViewModel.getIngredients(repository)
        }
    }
    Column(modifier = Modifier.fillMaxSize()) {
        ShoppingImageRow()
        ShoppingSearchRow(groceryListViewModel)
        IngredientList(
            groceryListViewModel
        )
    }
}

@Composable
fun IngredientList(
    groceryListViewModel: GroceryListViewModel,
) {
    val allListState = rememberLazyListState()
    val needState = rememberLazyListState()
    val groceryUIState = groceryListViewModel.groceryUIState.collectAsState()
    val ingredients = groceryUIState.value.ingredients
    val checkBoxStates = groceryUIState.value.checkBoxes
    if (groceryUIState.value.allListShowing) {
        val currentIngredients =
            if (groceryUIState.value.searching) groceryUIState.value.searchIngredients else groceryUIState.value.ingredients
        LazyColumn(state = allListState, content = {
            items(
                count = currentIngredients.size,
                key = { index -> currentIngredients[index].id },
                itemContent = { index ->
                    IngredientCard(
                        modifier = Modifier.fillMaxWidth(),
                        groceryListViewModel,
                        currentIngredients[index],
                        index,
                        index % 2 == 0,
                        groceryUIState.value.allListShowing
                    )
                }
            )
        })
    } else {
        val needList = ingredients.filterIndexed { index, _ ->
            !checkBoxStates[index]
        }
        val haveList = ingredients.filterIndexed { index, _ ->
            checkBoxStates[index]
        }
        LazyColumn(state = needState, content = {
            item {
                Text(
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
                    text = "Need", style = GroceryTitle
                )
            }
            items(
                count = needList.size,
                key = { index -> needList[index].id },
                itemContent = { index ->
                    NeedIngredientCard(
                        modifier = Modifier.fillMaxWidth(),
                        index,
                        true,
                        ingredients
                    )
                }
            )
            item {
                Text(
                    modifier = Modifier.padding(start = 16.dp, bottom = 12.dp, top = 20.dp),
                    text = "Have", style = GroceryTitle
                )
            }
            items(
                count = haveList.size,
                key = { index -> haveList[index].id },
                itemContent = { index ->
                    NeedIngredientCard(
                        modifier = Modifier.fillMaxWidth(),
                        index,
                        false,
                        ingredients
                    )
                }
            )
        })
    }
}


@Composable
fun IngredientCard(
    modifier: Modifier = Modifier,
    groceryListViewModel: GroceryListViewModel,
    ingredient: Ingredient,
    index: Int,
    isEven: Boolean,
    showCheckbox: Boolean,
) {
    val cardColor = if (isEven) CardDefaults.cardColors(containerColor = iconBackgroundColor) else
        CardDefaults.cardColors(containerColor = Color.White)
    val border = if (isEven) BorderStroke(width = 1.dp, color = Color.Black) else null
    val groceryUIState = groceryListViewModel.groceryUIState.collectAsState()
    val checkBoxStates = groceryUIState.value.checkBoxes
    val checked = checkBoxStates[index]
    Card(
        colors = cardColor,
        border = border,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
    ) {
        val style =
            if (checked) SpanStyle(textDecoration = TextDecoration.LineThrough) else SpanStyle()
        Row(modifier = Modifier.fillMaxSize()) {
            Text(
                text = AnnotatedString(ingredient.name, spanStyle = style),
                style = BodyLarge,
                color = Color.Black,
                modifier = Modifier.padding(16.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            if (showCheckbox) {
                Checkbox(checked = checked, onCheckedChange = {
                    val updatedList = checkBoxStates.toMutableList()
                    updatedList[index] = it
                    groceryListViewModel.updateCheckList(updatedList)
                })
            }
        }
    }
}

@Composable
fun NeedIngredientCard(
    modifier: Modifier = Modifier,
    index: Int,
    need: Boolean,
    ingredients: List<Ingredient>
) {
    val cardColor = CardDefaults.cardColors(containerColor = Color.White)
    val border = if (need) BorderStroke(width = 1.dp, color = Color.Black) else null
    val ingredient = ingredients[index]
    Card(
        colors = cardColor,
        border = border,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
    ) {
        val style =
            if (!need) SpanStyle(textDecoration = TextDecoration.LineThrough) else SpanStyle()
        Row(modifier = Modifier.fillMaxSize()) {
            Text(
                text = AnnotatedString(ingredient.name, spanStyle = style),
                style = BodyLarge,
                color = Color.Black,
                modifier = Modifier.padding(16.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
fun ShoppingImageRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(background1Color)
    ) {
        Image(
            modifier = Modifier.align(Alignment.Center),
            painter = painterResource(id = R.drawable.background1),
            contentDescription = null,
            contentScale = ContentScale.FillWidth
        )
    }
}

@Composable
fun ShoppingSearchRow(
    groceryListViewModel: GroceryListViewModel,
) {
    val searchText = rememberState {
        ""
    }
    val keyboard = LocalSoftwareKeyboardController.current
    val groceryUIState = groceryListViewModel.groceryUIState.collectAsState()
    val ingredients = groceryUIState.value.ingredients
    fun startSearch(searchString: String) {
        val searchIngredients = ingredients.filter { it.name.contains(searchString) }
        groceryListViewModel.setSearchIngredients(searchIngredients)
    }
    Row(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Box(modifier = Modifier
            .align(Alignment.CenterVertically)
            .clickable {
                if (searchText.value.isNotEmpty()) {
                    startSearch(searchText.value.trim())
                    keyboard?.hide()
                }
            }) {
            Icon(
                Icons.Filled.Search,
                contentDescription = "Search",
            )
        }
        SpacerW4()
        TextField(
            modifier = Modifier.weight(1f),
            value = searchText.value,
            onValueChange = {
                searchText.value = it
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (searchText.value.isNotEmpty()) {
                        startSearch(searchText.value.trim())
                        keyboard?.hide()
                    }
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
                groceryListViewModel.setSearching(false)
            }) {
                Icon(Icons.Filled.Clear, contentDescription = "Filter")
            }
        }
        FilterIcon(groceryListViewModel)
    }
}

@Composable
fun RowScope.FilterIcon(groceryListViewModel: GroceryListViewModel) {
    val groceryUIState = groceryListViewModel.groceryUIState.collectAsState()
    val allListShowing = groceryUIState.value.allListShowing
    val expanded = remember { mutableStateOf(false) }
    Box(modifier = Modifier.align(Alignment.CenterVertically)) {
        IconButton(onClick = {
            expanded.value = !expanded.value
        }) {
            Icon(Icons.Filled.FilterAlt, contentDescription = "Filter")
        }
        SpacerW4()
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            DropdownMenuItem(
                text = { Text("All") },
                leadingIcon = {
                    if (allListShowing) Icon(
                        imageVector = Icons.Filled.CheckBox,
                        contentDescription = "All"
                    )
                },
                onClick = {
                    groceryListViewModel.setAllShowing(true)
                    expanded.value = false
                }
            )
            DropdownMenuItem(
                text = { Text("Need/Have") },
                leadingIcon = {
                    if (!allListShowing) Icon(
                        imageVector = Icons.Filled.CheckBox,
                        contentDescription = "Need"
                    )
                },
                onClick = {
                    groceryListViewModel.setAllShowing(false)
                    expanded.value = false
                }
            )
        }
    }
}
