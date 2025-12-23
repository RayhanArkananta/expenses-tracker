package com.rayhan.expencestracker.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.rayhan.expencestracker.R
import com.rayhan.expencestracker.databinding.FragmentProfileBinding
import com.rayhan.expencestracker.features.login.LoginActivity

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    // Inisialisasi Database Reference
    private val databaseUrl = "https://expence-tra-4c82c-default-rtdb.asia-southeast1.firebasedatabase.app"
    private val dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("users")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        displayUserData()

        // 1. Logika Ubah Nama Profil
        binding.btnEditProfile.setOnClickListener {
            showEditNameDialog()
        }

        // 2. Logika Reset Password
        binding.btnResetPassword.setOnClickListener {
            sendResetPasswordEmail()
        }

        // 3. Logika Logout
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun displayUserData() {
        val user = auth.currentUser
        if (user != null) {
            // Data dari Firebase Auth
            binding.tvProfileName.text = user.displayName ?: "Pengguna"
            binding.tvProfileEmail.text = user.email

            // Ambil data tambahan (Gender, Phone, BirthDate) dari Realtime Database
            dbRef.child(user.uid).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val gender = snapshot.child("gender").value.toString()
                    val phone = snapshot.child("phone").value.toString()
                    val birthDate = snapshot.child("birthDate").value.toString()

                    // Update UI Nama & Email (jika di DB lebih akurat)
                    // Update Nomor Telepon & Tanggal Lahir (Opsional jika ada TextView-nya)
                    // binding.tvPhone.text = phone

                    // LOGIKA GANTI ICON PROFIL BERDASARKAN GENDER
                    if (gender == "Laki-laki") {
                        binding.ivProfilePic.setImageResource(R.drawable.ic_male_user)
                    } else if (gender == "Perempuan") {
                        binding.ivProfilePic.setImageResource(R.drawable.ic_female_user)
                    } else {
                        binding.ivProfilePic.setImageResource(R.drawable.ic_others)
                    }
                }
            }.addOnFailureListener {
                binding.ivProfilePic.setImageResource(R.drawable.ic_others)
            }
        }
    }

    private fun showEditNameDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Ubah Nama Profil")

        val input = EditText(requireContext())
        input.setText(auth.currentUser?.displayName)
        builder.setView(input)

        builder.setPositiveButton("Simpan") { _, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                updateProfileName(newName)
            }
        }
        builder.setNegativeButton("Batal", null)
        builder.show()
    }

    private fun updateProfileName(newName: String) {
        val user = auth.currentUser ?: return

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(newName)
            .build()

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Update di Realtime Database juga agar sinkron
                dbRef.child(user.uid).child("name").setValue(newName)

                binding.tvProfileName.text = newName
                Toast.makeText(context, "Nama berhasil diubah!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendResetPasswordEmail() {
        val email = auth.currentUser?.email
        if (email != null) {
            auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(context, "Link reset dikirim ke email Anda", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Gagal mengirim email reset", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}