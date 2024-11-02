package com.example.loggin_elecciones;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class crear_cuenta extends AppCompatActivity {
    private FirebaseFirestore db;

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
       db = FirebaseFirestore.getInstance();

       Button botonCrear = findViewById(R.id.boton_crear_cuenta);
       Button botonCancelar = findViewById(R.id.bt_cancel_create);

        botonCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(crear_cuenta.this, Loogin2.class);
                finish();
            }
        });
        botonCrear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText nombreCrear = findViewById(R.id.editText_nombre);
                EditText apellidosCrear = findViewById(R.id.editText_apellido);
                EditText contraseñaCrear = findViewById(R.id.editText_contraseña_loggin);
                EditText repetirContraseñaCrear = findViewById(R.id.editText_repetir_crear_contraseña);
                EditText codigoSissCrear = findViewById(R.id.editText_crear_siss);

                String nombre = nombreCrear.getText().toString();
                String apellidos = apellidosCrear.getText().toString();
                String contraseña = contraseñaCrear.getText().toString();
                String repetirContraseña = repetirContraseñaCrear.getText().toString();
                String siss = codigoSissCrear.getText().toString();

                if (!contraseñaCrear.getText().toString().equals(repetirContraseñaCrear.getText().toString())) {
                    Toast.makeText(crear_cuenta.this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (nombre.isEmpty() || apellidos.isEmpty() || contraseña.isEmpty() || repetirContraseña.isEmpty() || siss.isEmpty()) {
                    Toast.makeText(crear_cuenta.this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
                }
                insertarUsuarioFirestore(nombre, apellidos, siss, contraseña);
                Intent intent = new Intent(crear_cuenta.this, home_elector.class);
                startActivity(intent);
            }
        });


    }
    private void insertarUsuarioFirestore(String nombre, String apellidos, String siss, String contraseña) {
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nombre", nombre);
        usuario.put("apellidos", apellidos);
        usuario.put("siss", siss);
        usuario.put("contraseña", contraseña);

        db.collection("usuarios")
                .add(usuario)
            .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                // Manejar éxito
                @Override
                public void onSuccess(DocumentReference documentReference) {
                    Toast.makeText(crear_cuenta.this, "Usuario creado exitosamente", Toast.LENGTH_SHORT).show();
                }

            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(Exception e) {
                    // Manejar error
                    Toast.makeText(crear_cuenta.this, "Error al crear el usuario", Toast.LENGTH_SHORT).show();
                }

            });

    }

}