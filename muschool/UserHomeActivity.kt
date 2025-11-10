package com.example.muschool

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.database.*
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.ktx.database

class UserHomeActivity : AppCompatActivity() {

    private lateinit var ivUserAvatar: ImageView
    private lateinit var tvWelcome: TextView
    private val db: FirebaseDatabase by lazy { Firebase.database }
    private val usersRef: DatabaseReference by lazy { db.getReference("Users") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.user_profile_header)

        ivUserAvatar = findViewById(R.id.ivUserAvatar)
        tvWelcome = findViewById(R.id.tvWelcome)

        // Obtain the logged-in user id from auth/session
        val userId = intent.getStringExtra("userId") ?: return
        usersRef.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val name = snapshot.child("name").getValue(String::class.java) ?: "User"
                val url = snapshot.child("profileImageUrl").getValue(String::class.java)
                tvWelcome.text = "Welcome, $name"
                if (!url.isNullOrEmpty()) {
                    Glide.with(this@UserHomeActivity)
                        .load(url)
                        .centerCrop()
                        .into(ivUserAvatar)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
