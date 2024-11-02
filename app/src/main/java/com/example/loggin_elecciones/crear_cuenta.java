package com.example.loggin_elecciones;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.loggin_elecciones.databinding.ActivityCrearCuentaBinding;
import java.util.HashMap;
import java.util.Map;

public class crear_cuenta extends AppCompatActivity {
    private FirebaseFirestore db;
    private FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
    private ActivityCrearCuentaBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_crear_cuenta);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        // Infla el binding y establece el contenido de la actividad
        binding = ActivityCrearCuentaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
       if (currentUser != null) {
           String displayName = currentUser.getDisplayName();
           binding.textViewNombreFirebase.setText(displayName);
           String email = currentUser.getEmail();
           binding.textViewInstitucionalFirebase.setText(email);
       }


    }
}