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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("SameParameterValue", "RemoveCurlyBracesFromTemplate",
    "RemoveSingleExpressionStringTemplate"
)
class RetoAdapter(
    private val retos: List<DocumentSnapshot>,
    private val context: Context
) : RecyclerView.Adapter<RetoAdapter.RetoViewHolder>() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RetoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.challenges_list, parent, false)
        return RetoViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RetoViewHolder, position: Int) {
        val retosOrdenados = retos.sortedBy { document ->
            document.id.substringAfterLast(" ").toIntOrNull()?:0
            }
        val document = retosOrdenados[position]
        val retoName = document.id

        holder.viewName.text = retoName

        // Obtener el userId desde Firebase Auth
        val userId = auth.currentUser?.uid ?: return  // Salir si no hay usuario autenticado

        // Verificar si el reto tiene la colección de usuarios, si no, crearla con "completado: false"
        checkRetoCompletionStatus(userId, retoName) { isCompleted ->
            // Deshabilitar el reto si el reto anterior no está completado
            if (position > 0) {
                val previousRetoName = "DESAFÍO ${position}" // Asumimos nombres "Reto 1", "Reto 2", etc.
                checkRetoCompletionStatus(userId, previousRetoName) { previousRetoCompleted ->
                    holder.itemView.isClickable = previousRetoCompleted
                }
            } else {
                holder.itemView.isClickable = true // El primer reto siempre puede ser completado
            }

            // Si el reto está completado, el botón de "Completado" no debe ser presionado
            holder.viewName.isEnabled = !isCompleted
        }

        holder.itemView.setOnClickListener {
            val inflater = LayoutInflater.from(context)
            val detalleView = inflater.inflate(R.layout.reto_detalle, null)
            val campo1 = document.getString("campo1") ?: "Sin datos"
            val campo2 = document.getString("campo2") ?: "Sin datos"
            val campo3 = document.getString("campo3") ?: "Sin datos"

            val titulo = detalleView.findViewById<TextView>(R.id.tvTituloReto)
            titulo.text = "$retoName"
            val reto1 = detalleView.findViewById<TextView>(R.id.tvReto1)
            reto1.text = "RETO 1: $campo1"
            val reto2 = detalleView.findViewById<TextView>(R.id.tvReto2)
            reto2.text = "RETO 2: $campo2"
            val reto3 = detalleView.findViewById<TextView>(R.id.tvReto3)
            reto3.text = "RETO 3: $campo3"

// Mostrar dialogo de detalles
            val builder = AlertDialog.Builder(context)
            builder.setView(detalleView)
            builder.setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }

            val alertDialog = builder.create()

            val btnCompletado = detalleView.findViewById<Button>(R.id.btnCompletado)
            // Verificar si el reto está completado para cambiar el texto del botón
            checkRetoCompletionStatus(userId, retoName) { isCompleted ->
                if (isCompleted) {
                    btnCompletado.text = "COMPLETADO" // Si el reto ya está completado

                } else {
                    btnCompletado.text = "COMPLETAR" // Si el reto no está completado
                    btnCompletado.isEnabled = true // Habilitar el botón para completar el reto
                }
            }

            btnCompletado.setOnClickListener {
                // Comprobar si el reto ya fue completado
                checkRetoCompletionStatus(userId, retoName) { isCompleted ->
                    if (isCompleted) {
                        // Si el reto ya está completado, mostrar un mensaje
                        Toast.makeText(context, "$retoName completado, avance al siguiente", Toast.LENGTH_SHORT).show()

                        // Pasar al siguiente reto
                        alertDialog.dismiss()

                        // Actualizar todos los siguientes retos de manera recursiva
                        enableSubsequentRetos(userId, position)
                    } else {
                        // Si el reto no está completado, marcarlo como completado
                        Toast.makeText(context, "$retoName completado", Toast.LENGTH_SHORT).show()

                        // Marcar el reto como completado en Firestore
                        updateRetoStatus(retoName, true, userId)

                        // Cierra el dialogo
                        alertDialog.dismiss()

                        // Notificar que el reto fue completado y permitir interacciones con el siguiente reto
                        notifyItemChanged(position)

                        // Actualizar todos los siguientes retos de manera recursiva
                        enableSubsequentRetos(userId, position)
                    }
                }
            }

            alertDialog.show()
        }
    }

    private fun checkRetoCompletionStatus(userId: String, retoName: String, callback: (Boolean) -> Unit) {
        val retoRef = db.collection("retos")
            .document(retoName)
            .collection("usuarios")
            .document(userId)

        retoRef.get().addOnSuccessListener { document ->
            // Si no existe el documento, lo creamos con "completado = false"
            if (!document.exists()) {
                retoRef.set(mapOf("completado" to false))
                    .addOnSuccessListener {
                        callback(false) // Retornar que no está completado
                    }
                    .addOnFailureListener {
                        callback(false)
                    }
            } else {
                val isCompleted = document.getBoolean("completado") ?: false
                callback(isCompleted)
            }
        }.addOnFailureListener {
            callback(false)
        }
    }

    private fun updateRetoStatus(retoId: String, isCompleted: Boolean, userId: String) {
        val retoRef = db.collection("retos").document(retoId).collection("usuarios").document(userId)

        // Crear o actualizar el documento del usuario en la subcolección 'usuarios'
        retoRef.set(mapOf("completado" to isCompleted), com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error al marcar el reto como completado", Toast.LENGTH_SHORT).show()
            }
    }

    // Actualiza los retos posteriores si el reto actual fue completado
    private fun enableSubsequentRetos(userId: String, position: Int) {
        for (i in position + 1 until itemCount) {
            val nextRetoName = "DESAFÍO ${i + 1}"
            checkRetoCompletionStatus(userId, nextRetoName) { nextRetoCompleted ->
                if (!nextRetoCompleted) {
                    // Si el reto siguiente NO se ha completado, habilitarlo
                    notifyItemChanged(i)
                }
            }
        }
    }

    override fun getItemCount(): Int = retos.size

    class RetoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val viewName: TextView = itemView.findViewById(R.id.verReto)
        }
}