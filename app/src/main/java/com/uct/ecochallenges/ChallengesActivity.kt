@file:Suppress("DEPRECATION")
package com.uct.ecochallenges

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

@Suppress("UNUSED_ANONYMOUS_PARAMETER")
class ChallengesActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var retosAdapter: RetoAdapter
    private val retosList = mutableListOf<DocumentSnapshot>()

    private lateinit var googleSignInClient: GoogleSignInClient
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_challenges)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Configurar RecyclerView
        val recyclerView: RecyclerView = findViewById(R.id.recyclerCompras)
        retosAdapter = RetoAdapter(retosList, this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = retosAdapter

        // Cargar datos desde Firestore
        loadChallenges()

        val salir : FloatingActionButton = findViewById(R.id.btnSalir)

        salir.setOnClickListener {
            showLogoutConfirmationDialog()
        }

    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadChallenges() {
        val retosRef = db.collection("retos") // Referencia a la colección "retos"

        // Obtener los datos de Firestore
        retosRef.addSnapshotListener { snapshots, e ->
            if (e != null) {
                Toast.makeText(this, "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            // Actualizar la lista con los documentos obtenidos
            if (snapshots != null) {
                retosList.clear()
                retosList.addAll(snapshots.documents)
                retosAdapter.notifyDataSetChanged() // Notificar al adaptador
            }
        }
    }

    private fun showLogoutConfirmationDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Cerrar sesión")
        builder.setMessage("¿Estás seguro que quieres cerrar sesión?")
        builder.setPositiveButton("Aceptar") { dialog, which ->
            auth.signOut()
            googleSignInClient.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Opcional: cerrar esta actividad
        }
        builder.setNegativeButton("Cancelar") { dialog, which ->
        }
        builder.show()
        }
}