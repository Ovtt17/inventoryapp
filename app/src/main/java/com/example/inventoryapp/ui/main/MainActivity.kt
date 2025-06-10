package com.example.inventoryapp.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.adapter.ProductAdapter
import com.example.inventoryapp.data.api.ApiClient
import com.example.inventoryapp.model.Product
import com.example.inventoryapp.ui.login.LoginActivity
import com.example.inventoryapp.utils.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var products: MutableList<Product>
    private lateinit var productAdapter: ProductAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        var user = prefs.getString("user", null)

        if (user == null) {
            // No hay sesión, redirigir al login
            startActivity(Intent(this, LoginActivity::class.java))
            return
        }

        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        CoroutineScope(Dispatchers.Main).launch {
            products = loadProducts()
            setupRecyclerView()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                handleAddProduct()
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        productAdapter = ProductAdapter(
            this,
            products,
            onEditProduct = { product -> handleEditProduct(product) },
            onDeleteProduct = { product -> handleDeleteProduct(product) },
            onItemClick = { product -> handleProductClick(product) }
        )
        recyclerView.adapter = productAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun handleAddProduct() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_product_form, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Agregar Producto")
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val name = dialogView.findViewById<EditText>(R.id.etName).text.toString()
                val price =
                    dialogView.findViewById<EditText>(R.id.etPrice).text.toString().toDoubleOrNull()
                val description =
                    dialogView.findViewById<EditText>(R.id.etDescription).text.toString()
                val stock =
                    dialogView.findViewById<EditText>(R.id.etStock).text.toString().toIntOrNull()
                val category = dialogView.findViewById<EditText>(R.id.etCategory).text.toString()

                if (name.isEmpty() || price == null || description.isEmpty() || stock == null || category.isEmpty()) {
                    Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val newProduct = Product(
                        id = 0, // El ID será asignado por el servidor
                        name = name,
                        price = price,
                        description = description,
                        stock = stock,
                        category = category
                    )

                    CoroutineScope(Dispatchers.IO).launch {
                        val productService = ApiClient.getProductService()
                        val response = productService.addProduct(newProduct)
                        if (response.isSuccessful) {
                            products.add(response.body()!!)
                            runOnUiThread {
                                productAdapter.notifyItemInserted(products.size - 1)
                                Toast.makeText(
                                    this@MainActivity,
                                    "Producto agregado: $name",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dialog.dismiss()
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Error al agregar producto",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private suspend fun loadProducts(): MutableList<Product> {
        val productService = ApiClient.getProductService()
        val response = productService.getProducts()
        return if (response.isSuccessful) {
            response.body() ?: mutableListOf()
        } else {
            runOnUiThread {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("Error")
                    .setMessage("No se pudieron cargar los productos.")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
            }
            mutableListOf()
        }
    }

    private fun handleEditProduct(product: Product) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_product_form, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Editar Producto")
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val nameField = dialogView.findViewById<EditText>(R.id.etName)
            val priceField = dialogView.findViewById<EditText>(R.id.etPrice)
            val descriptionField = dialogView.findViewById<EditText>(R.id.etDescription)
            val stockField = dialogView.findViewById<EditText>(R.id.etStock)
            val categoryField = dialogView.findViewById<EditText>(R.id.etCategory)

            // Pre-fill fields with product details
            nameField.setText(product.name)
            priceField.setText(product.price.toString())
            descriptionField.setText(product.description)
            stockField.setText(product.stock.toString())
            categoryField.setText(product.category)

            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val updatedName = nameField.text.toString()
                val updatedPrice = priceField.text.toString().toDoubleOrNull()
                val updatedDescription = descriptionField.text.toString()
                val updatedStock = stockField.text.toString().toIntOrNull()
                val updatedCategory = categoryField.text.toString()

                if (updatedName.isEmpty() || updatedPrice == null || updatedDescription.isEmpty() || updatedStock == null || updatedCategory.isEmpty()) {
                    Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    val updatedProduct = product.copy(
                        name = updatedName,
                        price = updatedPrice,
                        description = updatedDescription,
                        stock = updatedStock,
                        category = updatedCategory
                    )

                    CoroutineScope(Dispatchers.IO).launch {
                        val productService = ApiClient.getProductService()
                        val response = productService.updateProduct(product.id, updatedProduct)
                        if (response.isSuccessful) {
                            val index = products.indexOf(product)
                            if (index != -1) {
                                products[index] = response.body()!!
                                runOnUiThread {
                                    productAdapter.notifyItemChanged(index)
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Producto actualizado: $updatedName",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    dialog.dismiss()
                                }
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(
                                    this@MainActivity,
                                    "Error al actualizar producto",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                }
            }
        }

        dialog.show()
    }

    private fun handleDeleteProduct(product: Product) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Eliminar Producto")
            .setMessage("¿Está seguro de que desea eliminar ${product.name}?")
            .setPositiveButton("Eliminar") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    val productService = ApiClient.getProductService()
                    val response = productService.deleteProduct(product.id)

                    if (response.isSuccessful) {
                        val index = products.indexOfFirst { it.id == product.id }
                        if (index != -1) {
                            products.removeAt(index)
                            runOnUiThread {
                                productAdapter.notifyItemRemoved(index)
                                Toast.makeText(
                                    this@MainActivity,
                                    "${product.name} eliminado",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(
                                this@MainActivity,
                                "Error al eliminar el producto",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.show()
    }

    private fun handleProductClick(product: Product) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_product_details, null)
        dialogView.findViewById<TextView>(R.id.nameTextView).text = product.name
        dialogView.findViewById<TextView>(R.id.priceTextView).text = product.price.toString()
        dialogView.findViewById<TextView>(R.id.descriptionTextView).text = product.description
        dialogView.findViewById<TextView>(R.id.stockTextView).text = product.stock.toString()
        dialogView.findViewById<TextView>(R.id.categoryTextView).text = product.category

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Detalles del Producto")
            .setPositiveButton("Cerrar", null)
            .create()
            .show()
    }

    private fun logout() {
        SessionManager.clearUserSession(this)

        // Redirige al LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}