// Generated by view binder compiler. Do not edit!
package com.example.loggin_elecciones.databinding;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.viewbinding.ViewBinding;
import androidx.viewbinding.ViewBindings;
import com.example.loggin_elecciones.R;
import com.google.android.gms.common.SignInButton;
import java.lang.NullPointerException;
import java.lang.Override;
import java.lang.String;

public final class ActivityLoogin2Binding implements ViewBinding {
  @NonNull
  private final ConstraintLayout rootView;

  @NonNull
  public final LinearLayout administradorContenedor;

  @NonNull
  public final Button btAdministrador;

  @NonNull
  public final Button btDocente;

  @NonNull
  public final Button btEstudiante;

  @NonNull
  public final LinearLayout contenedorLoggin;

  @NonNull
  public final EditText contraseAAdministrador;

  @NonNull
  public final ImageView imagenFcyt;

  @NonNull
  public final ImageView imagenUmss;

  @NonNull
  public final TextView loginText;

  @NonNull
  public final ConstraintLayout main;

  @NonNull
  public final Space sepImages;

  @NonNull
  public final SignInButton signInButton;

  @NonNull
  public final TextView textViewEstudiate;

  @NonNull
  public final EditText userAdministrador;

  private ActivityLoogin2Binding(@NonNull ConstraintLayout rootView,
      @NonNull LinearLayout administradorContenedor, @NonNull Button btAdministrador,
      @NonNull Button btDocente, @NonNull Button btEstudiante,
      @NonNull LinearLayout contenedorLoggin, @NonNull EditText contraseAAdministrador,
      @NonNull ImageView imagenFcyt, @NonNull ImageView imagenUmss, @NonNull TextView loginText,
      @NonNull ConstraintLayout main, @NonNull Space sepImages, @NonNull SignInButton signInButton,
      @NonNull TextView textViewEstudiate, @NonNull EditText userAdministrador) {
    this.rootView = rootView;
    this.administradorContenedor = administradorContenedor;
    this.btAdministrador = btAdministrador;
    this.btDocente = btDocente;
    this.btEstudiante = btEstudiante;
    this.contenedorLoggin = contenedorLoggin;
    this.contraseAAdministrador = contraseAAdministrador;
    this.imagenFcyt = imagenFcyt;
    this.imagenUmss = imagenUmss;
    this.loginText = loginText;
    this.main = main;
    this.sepImages = sepImages;
    this.signInButton = signInButton;
    this.textViewEstudiate = textViewEstudiate;
    this.userAdministrador = userAdministrador;
  }

  @Override
  @NonNull
  public ConstraintLayout getRoot() {
    return rootView;
  }

  @NonNull
  public static ActivityLoogin2Binding inflate(@NonNull LayoutInflater inflater) {
    return inflate(inflater, null, false);
  }

  @NonNull
  public static ActivityLoogin2Binding inflate(@NonNull LayoutInflater inflater,
      @Nullable ViewGroup parent, boolean attachToParent) {
    View root = inflater.inflate(R.layout.activity_loogin2, parent, false);
    if (attachToParent) {
      parent.addView(root);
    }
    return bind(root);
  }

  @NonNull
  public static ActivityLoogin2Binding bind(@NonNull View rootView) {
    // The body of this method is generated in a way you would not otherwise write.
    // This is done to optimize the compiled bytecode for size and performance.
    int id;
    missingId: {
      id = R.id.administrador_contenedor;
      LinearLayout administradorContenedor = ViewBindings.findChildViewById(rootView, id);
      if (administradorContenedor == null) {
        break missingId;
      }

      id = R.id.bt_Administrador;
      Button btAdministrador = ViewBindings.findChildViewById(rootView, id);
      if (btAdministrador == null) {
        break missingId;
      }

      id = R.id.bt_Docente;
      Button btDocente = ViewBindings.findChildViewById(rootView, id);
      if (btDocente == null) {
        break missingId;
      }

      id = R.id.bt_Estudiante;
      Button btEstudiante = ViewBindings.findChildViewById(rootView, id);
      if (btEstudiante == null) {
        break missingId;
      }

      id = R.id.contenedor_loggin;
      LinearLayout contenedorLoggin = ViewBindings.findChildViewById(rootView, id);
      if (contenedorLoggin == null) {
        break missingId;
      }

      id = R.id.contraseñaAdministrador;
      EditText contraseAAdministrador = ViewBindings.findChildViewById(rootView, id);
      if (contraseAAdministrador == null) {
        break missingId;
      }

      id = R.id.imagen_fcyt;
      ImageView imagenFcyt = ViewBindings.findChildViewById(rootView, id);
      if (imagenFcyt == null) {
        break missingId;
      }

      id = R.id.imagen_umss;
      ImageView imagenUmss = ViewBindings.findChildViewById(rootView, id);
      if (imagenUmss == null) {
        break missingId;
      }

      id = R.id.login_text;
      TextView loginText = ViewBindings.findChildViewById(rootView, id);
      if (loginText == null) {
        break missingId;
      }

      ConstraintLayout main = (ConstraintLayout) rootView;

      id = R.id.sep_images;
      Space sepImages = ViewBindings.findChildViewById(rootView, id);
      if (sepImages == null) {
        break missingId;
      }

      id = R.id.signInButton;
      SignInButton signInButton = ViewBindings.findChildViewById(rootView, id);
      if (signInButton == null) {
        break missingId;
      }

      id = R.id.textView_estudiate;
      TextView textViewEstudiate = ViewBindings.findChildViewById(rootView, id);
      if (textViewEstudiate == null) {
        break missingId;
      }

      id = R.id.userAdministrador;
      EditText userAdministrador = ViewBindings.findChildViewById(rootView, id);
      if (userAdministrador == null) {
        break missingId;
      }

      return new ActivityLoogin2Binding((ConstraintLayout) rootView, administradorContenedor,
          btAdministrador, btDocente, btEstudiante, contenedorLoggin, contraseAAdministrador,
          imagenFcyt, imagenUmss, loginText, main, sepImages, signInButton, textViewEstudiate,
          userAdministrador);
    }
    String missingId = rootView.getResources().getResourceName(id);
    throw new NullPointerException("Missing required view with ID: ".concat(missingId));
  }
}
