package com.example.myapplication

import android.annotation.SuppressLint
import android.os.Bundle
import android.print.PrintAttributes.Margins
import android.text.style.BackgroundColorSpan
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.squareup.moshi.Json

import okhttp3.OkHttpClient
import okhttp3.Request
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.IOException

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

import android.util.Log
import androidx.compose.material3.Button
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationCompat.Style
import javax.sql.RowSetListener
import kotlin.math.log


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApp();
        }
    }
}

data class Item(
    @Json(name = "id") val id: Int,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String
)

object ApiClient {
    private val client = OkHttpClient()
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val itemAdapter = moshi.adapter<List<Item>>(Types.newParameterizedType(List::class.java, Item::class.java))

    suspend fun fetchItems(): List<Item> {
        val request = Request.Builder()
            .url("https://66a2309c967c89168f1f2086.mockapi.io/mobile/v1/items")
            .build()


        return withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected code $response")

                response.body?.string()?.let { responseBody ->
                    Log.d("Check ", response.body.toString());
                    itemAdapter.fromJson(responseBody) ?: emptyList()
                } ?: emptyList()
            }
        }
    }
}

class ItemViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<Item>>(emptyList())
    val items: StateFlow<List<Item>> = _items

    init {
        fetchItems()
    }

    private fun fetchItems() {
        viewModelScope.launch {
            try {
                val fetchedItems = ApiClient.fetchItems()
                _items.value = fetchedItems
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListScreen(viewModel: ItemViewModel, onItemClick: (Item) -> Unit) {
    val items by viewModel.items.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Item List") })
        },
        content = { paddingValues ->
            LazyColumn(
                contentPadding = paddingValues,
                modifier = Modifier.fillMaxSize()
            ) {
                items(items) { item ->
                    ItemRow(item, onItemClick)
                }
            }
        }
    )
}

@Composable
fun ItemRow(item: Item, onItemClick: (Item) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
//            .clickable { onItemClick(item) }
    ) {
//        Column(modifier = Modifier.padding(16.dp)) {
//            Text(text = item.title, style = MaterialTheme.typography.titleSmall)
//            Text(text = item.description, style = MaterialTheme.typography.bodyLarge)
//        }
        Row(modifier = Modifier.padding(16.dp)) {
//            Text(text = item.title, style = MaterialTheme.typography.titleSmall, color = Color.Blue)
//            Text(text = item.description, style = MaterialTheme.typography.bodyLarge, color = Color.Red)
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = item.title, style = MaterialTheme.typography.titleSmall, color = Color.Blue)
                Text(text = item.description, style = MaterialTheme.typography.bodyLarge, color = Color.Red)
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Button(onClick = { onItemClick(item)  }) {
                    Text(item.title, color = Color.Green)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsScreen(item: Item, onNavigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text(text = item.title, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = item.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    )
}

@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun MyApp() {
    val navController = rememberNavController()
    val viewModel: ItemViewModel = viewModel()

    NavHost(navController, startDestination = "list") {
        composable("list") {
            ItemListScreen(viewModel) { selectedItem ->
                navController.navigate("details/${selectedItem.id}")
            }
        }
        composable("details/{itemId}") { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")?.toIntOrNull()
            val item = viewModel.items
                .value.find { it.id == itemId }
            item?.let {
                ItemDetailsScreen(it) { navController.popBackStack() }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApp();
}