package com.example.inventoryapp.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.MenuInflater
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.inventoryapp.R
import com.example.inventoryapp.databinding.ProductItemBinding
import com.example.inventoryapp.model.Product

class ProductAdapter(
    private val context: Context,
    private val products: MutableList<Product>,
    private val onEditProduct: (Product) -> Unit,
    private val onDeleteProduct: (Product) -> Unit,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(private val binding: ProductItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.nameTextView.text = product.name
            binding.priceTextView.text = product.price.toString()
            binding.descriptionTextView.text = product.description
            binding.stockTextView.text = product.stock.toString()
            binding.categoryTextView.text = product.category

            // Handle item click
            binding.root.setOnClickListener {
                onItemClick(product)
            }

            // Handle long click for edit/delete
            binding.root.setOnLongClickListener {
                val menu = PopupMenu(context, binding.root)
                val inflater: MenuInflater = menu.menuInflater
                inflater.inflate(R.menu.menu_context, menu.menu)

                menu.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.edit_product -> {
                            onEditProduct(product)
                            true
                        }
                        R.id.delete_product -> {
                            onDeleteProduct(product)
                            true
                        }
                        else -> false
                    }
                }
                menu.show()
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ProductItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount(): Int = products.size
}