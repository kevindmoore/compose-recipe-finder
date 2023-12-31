package com.kodeco.recipefinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.kodeco.recipefinder.data.Prefs
import com.kodeco.recipefinder.data.Repository
import com.kodeco.recipefinder.data.database.RecipeDatabase
import com.kodeco.recipefinder.ui.MainScreen
import com.kodeco.recipefinder.ui.RecipeDetails
import com.kodeco.recipefinder.ui.theme.RecipeFinderTheme

val LocalNavigatorProvider =
    compositionLocalOf<NavHostController> { error("No navigation provided") }

val LocalRepositoryProvider =
    compositionLocalOf<Repository> { error("No repository provided") }

val LocalPrefsProvider =
    compositionLocalOf<Prefs> { error("No prefs provided") }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RecipeFinderTheme(darkTheme = false) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    val navController = rememberNavController()
                    val repository = remember {
                        Repository(
                            Room.databaseBuilder(
                                context,
                                RecipeDatabase::class.java,
                                "Recipes"
                            ).build()
                        )
                    }
                    val prefs = remember { Prefs(context) }
                    CompositionLocalProvider(
                        LocalNavigatorProvider provides navController,
                        LocalRepositoryProvider provides repository,
                        LocalPrefsProvider provides prefs,
                    ) {
                        NavHost(navController = navController, startDestination = "main") {
                            composable("main") { MainScreen() }
                            composable(
                                "details/{recipeId}",
                                arguments = listOf(navArgument("recipeId") {
                                    type = NavType.IntType
                                })
                            ) { backStackEntry ->
                                RecipeDetails(
                                    recipeId = backStackEntry.arguments?.getInt("recipeId") ?: 0
                                )
                            }
                            composable(
                                "bookmarks/{recipeId}",
                                arguments = listOf(navArgument("recipeId") {
                                    type = NavType.IntType
                                })
                            ) { backStackEntry ->
                                RecipeDetails(
                                    databaseRecipeId = backStackEntry.arguments?.getInt("recipeId") ?: 0
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainPreview() {
    RecipeFinderTheme {
        MainScreen()
    }
}