package com.example.muschool

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.imageview.ShapeableImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.android.material.transition.platform.MaterialSharedAxis

class AdminProfileActivity : AppCompatActivity() {

    private lateinit var imgProfile: ShapeableImageView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvUsername: TextView
    private lateinit var tvRole: TextView

    private lateinit var auth: FirebaseAuth
    private lateinit var userRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✨ Smooth Material transition (side-to-side)
        window.enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true)
        window.returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false)
        window.sharedElementsUseOverlay = false

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_profile)

        // 🔹 Initialize Views
        imgProfile = findViewById(R.id.imgProfile)
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvEmail)
        tvPhone = findViewById(R.id.tvPhone)
        tvUsername = findViewById(R.id.tvUsername)
        tvRole = findViewById(R.id.tvRole)

        auth = FirebaseAuth.getInstance()
        val currentEmail = auth.currentUser?.email

        if (currentEmail != null) {
            userRef = FirebaseDatabase.getInstance().getReference("Users")
            userRef.orderByChild("email").equalTo(currentEmail)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(this@AdminProfileActivity, "Profile not found", Toast.LENGTH_SHORT).show()
                            return
                        }

                        for (child in snapshot.children) {
                            Log.d("FIREBASE_ADMIN", child.toString())

                            val name = child.child("name").getValue(String::class.java) ?: "N/A"
                            val email = child.child("email").getValue(String::class.java) ?: "N/A"
                            val phone = child.child("phone").getValue(String::class.java) ?: "N/A"
                            val username = child.child("username").getValue(String::class.java) ?: "N/A"
                            val role = child.child("role").getValue(String::class.java) ?: "N/A"
                            val profileUrl = child.child("profileImageUrl").getValue(String::class.java)

                            // 🧾 Populate TextViews
                            tvName.text = "👤 Name: $name"
                            tvEmail.text = "📧 Email: $email"
                            tvPhone.text = "📞 Phone: $phone"
                            tvUsername.text = "🆔 Username: $username"
                            tvRole.text = "🎖️ Role: $role"

                            // 🖼️ Load Profile Picture
                            if (!profileUrl.isNullOrEmpty()) {
                                Glide.with(this@AdminProfileActivity)
                                    .load(profileUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                                    .placeholder(R.drawable.baseline_person_24)
                                    .error(R.drawable.baseline_person_24)
                                    .circleCrop()
                                    .into(imgProfile)
                            } else {
                                imgProfile.setImageResource(R.drawable.baseline_person_24)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@AdminProfileActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
        }
    }
}
