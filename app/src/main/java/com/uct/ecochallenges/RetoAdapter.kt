package com.uct.ecochallenges

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("SameParameterValue")
class RetoAdapter(
    private val retos: List<DocumentSnapshot>,
    private val context: Context
) : RecyclerView.Adapter<RetoAdapter.RetoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RetoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.challenges_list, parent, false)
        return RetoViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RetoViewHolder, position: Int) {
        val document = retos[position]
        val retoName = document.id

        holder.viewName.text = retoName

        // Elimina la lógica de deshabilitar los retos basados en el estado de completado

        holder.itemView.setOnClickListener {
            val inflater = LayoutInflater.from(context)
            val detalleView = inflater.inflate(R.layout.reto_detalle, null)
            val campo1 = document.getString("campo1") ?: "Sin datos"
            val campo2 = document.getString("campo2") ?: "Sin datos"
            val campo3 = document.getString("campo3") ?: "Sin datos"

            val titulo = detalleView.findViewById<TextView>(R.id.tvTituloReto)
            titulo.text = "Detalles del $retoName"
            val reto1 = detalleView.findViewById<TextView>(R.id.tvReto1)
            reto1.text = "Reto 1: $campo1"
            val reto2 = detalleView.findViewById<TextView>(R.id.tvReto2)
            reto2.text = "Reto 2: $campo2"
            val reto3 = detalleView.findViewById<TextView>(R.id.tvReto3)
            reto3.text = "Reto 3: $campo3"

            // Mostrar dialogo de detalles
            val builder = AlertDialog.Builder(context)
            builder.setView(detalleView)
            builder.setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }

            val alertDialog = builder.create()

            val btnCompletado = detalleView.findViewById<Button>(R.id.btnCompletado)
            btnCompletado.setOnClickListener {
                // Muestra un mensaje toast cuando se marca el reto como completado
                Toast.makeText(context, "$retoName completado", Toast.LENGTH_SHORT).show()

                // Marcar el reto como completado en Firestore
                updateRetoStatus(document.id, true)

                // Cierra el dialogo
                alertDialog.dismiss()

            }

            alertDialog.show()
        }
    }

    private fun updateRetoStatus(retoId: String, isCompleted: Boolean) {
        val retoRef = FirebaseFirestore.getInstance().collection("retos").document(retoId)

        // Actualiza el estado del reto a completado
        retoRef.update("completado", isCompleted)
    }

    override fun getItemCount(): Int = retos.size

    class RetoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val viewName: TextView = itemView.findViewById(R.id.verReto)
    }
}