package com.example.whatsapp.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.whatsapp.R;
import com.example.whatsapp.config.ConfiguracaoFirebase;
import com.example.whatsapp.model.Usuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText campoSenha;
    private EditText campoEmail;
    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        campoSenha = findViewById(R.id.textInputSenhaLogin);
        campoEmail = findViewById(R.id.editLoginEmail);
    }

    public void logarUsuario( Usuario usuario ){

        autenticacao.signInWithEmailAndPassword(
                usuario.getEmail(), usuario.getSenha()
        ).addOnCompleteListener(task -> {

            if ( task.isSuccessful() ){
                abrirTelaPrincipal();

            }else {

                String excecao = "";
                try {
                    throw task.getException();
                }catch ( FirebaseAuthInvalidUserException e){
                    excecao = "Usuário não está cadastrado.";
                }catch ( FirebaseAuthInvalidCredentialsException e){
                    excecao = "E-mail e senha não correspondem a um usuário cadastrado.";
                }catch ( Exception e ){
                    excecao = "Erro ao cadastrar usuário: " + e.getMessage();
                    e.printStackTrace();
                }
                Toast.makeText(LoginActivity.this, "Erro ao fazer login!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void validarAutenticacaoUsuario(View view){

        String textoEmail = campoEmail.getText().toString();
        String textoSenha = Objects.requireNonNull(campoSenha.getText()).toString();

        if ( !textoEmail.isEmpty()){
            if ( !textoSenha.isEmpty()){

                Usuario usuario = new Usuario();
                usuario.setEmail( textoEmail );
                usuario.setSenha( textoSenha );

                logarUsuario( usuario );

            }else {
                Toast.makeText(LoginActivity.this, "Preencha a senha!",
                        Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast.makeText(LoginActivity.this, "Preencha o e-mail!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser usuarioAtual = autenticacao.getCurrentUser();
        if ( usuarioAtual != null ){
            abrirTelaPrincipal();
        }
    }

    public void abrirTelaCadastro(View view){
        Intent intent = new Intent(LoginActivity.this, CadastroActivity.class);
        startActivity( intent );
    }
    public void abrirTelaPrincipal(){
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity( intent );
    }
}