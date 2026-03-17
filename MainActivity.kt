package com.example.trabajofinal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

data class Libro(
    val id: Int = 0,
    val titulo: String,
    val codigo: String,
    val editorial: String,
    val autor: String,
    val anio: String
)

interface LibroApiService {
    @GET("libros")
    suspend fun obtenerLibros(): List<Libro>

    @POST("libros")
    suspend fun guardarLibro(@Body libro: Libro): Libro

    @PUT("libros/{id}")
    suspend fun editarLibro(@Path("id") id: Int, @Body libro: Libro): Libro

    @DELETE("libros/{id}")
    suspend fun borrarLibro(@Path("id") id: Int)
}

object RetrofitClient {
    val api: LibroApiService by lazy {
        Retrofit.Builder()
            .baseUrl("http://146.190.154.216/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LibroApiService::class.java)
    }
}

class LibroViewModel : ViewModel() {
    var libros = mutableStateListOf<Libro>()
    
    suspend fun cargarLibros() {
        try {
            val lista = RetrofitClient.api.obtenerLibros()
            libros.clear()
            libros.addAll(lista)
        } catch (e: Exception) {}
    }

    suspend fun agregar(libro: Libro) {
        try {
            RetrofitClient.api.guardarLibro(libro)
            cargarLibros()
        } catch (e: Exception) {}
    }

    suspend fun editar(id: Int, libro: Libro) {
        try {
            RetrofitClient.api.editarLibro(id, libro)
            cargarLibros()
        } catch (e: Exception) {}
    }

    suspend fun eliminar(id: Int) {
        try {
            RetrofitClient.api.borrarLibro(id)
            cargarLibros()
        } catch (e: Exception) {}
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    LibrosApp()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibrosApp(vm: LibroViewModel = viewModel()) {
    val scope = rememberCoroutineScope()
    var mostrarDialogo by remember { mutableStateOf(false) }
    var libroEditando by remember { mutableStateOf<Libro?>(null) }

    var titulo by remember { mutableStateOf("") }
    var codigo by remember { mutableStateOf("") }
    var editorial by remember { mutableStateOf("") }
    var autor by remember { mutableStateOf("") }
    var anio by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        vm.cargarLibros()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Biblioteca UPI") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                libroEditando = null
                titulo = ""; codigo = ""; editorial = ""; autor = ""; anio = ""
                mostrarDialogo = true 
            }) {
                Icon(Icons.Default.Add, contentDescription = null)
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
            items(vm.libros) { libro ->
                Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(libro.titulo, style = MaterialTheme.typography.headlineSmall)
                        Text("Autor: ${libro.autor}")
                        Text("Código: ${libro.codigo}")
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            IconButton(onClick = { 
                                libroEditando = libro
                                titulo = libro.titulo; codigo = libro.codigo
                                editorial = libro.editorial; autor = libro.autor; anio = libro.anio
                                mostrarDialogo = true 
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = null)
                            }
                            IconButton(onClick = { scope.launch { vm.eliminar(libro.id) } }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }

        if (mostrarDialogo) {
            AlertDialog(
                onDismissRequest = { mostrarDialogo = false },
                title = { Text(if (libroEditando == null) "Nuevo Libro" else "Editar Libro") },
                text = {
                    Column {
                        TextField(value = titulo, onValueChange = { titulo = it }, label = { Text("Título") })
                        TextField(value = codigo, onValueChange = { codigo = it }, label = { Text("Código") })
                        TextField(value = editorial, onValueChange = { editorial = it }, label = { Text("Editorial") })
                        TextField(value = autor, onValueChange = { autor = it }, label = { Text("Autor") })
                        TextField(value = anio, onValueChange = { anio = it }, label = { Text("Año") })
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        val lib = Libro(
                            id = libroEditando?.id ?: 0,
                            titulo = titulo, codigo = codigo,
                            editorial = editorial, autor = autor, anio = anio
                        )
                        scope.launch {
                            if (libroEditando == null) vm.agregar(lib)
                            else vm.editar(lib.id, lib)
                            mostrarDialogo = false
                        }
                    }) { Text("Guardar") }
                }
            )
        }
    }
}
