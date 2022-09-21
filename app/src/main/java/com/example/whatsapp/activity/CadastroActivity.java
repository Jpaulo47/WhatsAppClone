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
import com.example.whatsapp.helper.Base64Custom;
import com.example.whatsapp.helper.UsuarioFirebase;
import com.example.whatsapp.model.Usuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

import java.util.Objects;

public class CadastroActivity extends AppCompatActivity {

    private TextInputEditText campoSenha;
    private EditText campoNome, campoEmail;
    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cadastro);

        campoNome  = findViewById(R.id.editCadastroNome);
        campoEmail = findViewById(R.id.editCadastroEmail);
        campoSenha = findViewById(R.id.textSenhaCadastro);
    }

    public void cadastrarUsuario(Usuario usuario){
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.createUserWithEmailAndPassword(
                usuario.getEmail(),usuario.getSenha()

        ).addOnCompleteListener(this, task -> {

            if ( task.isSuccessful()){
                Intent intent = new Intent(CadastroActivity.this, MainActivity.class);
                startActivity( intent );
                Toast.makeText(CadastroActivity.this,
                        "Sucesso ao cadastrar usuário",
                        Toast.LENGTH_SHORT).show();
                UsuarioFirebase.atualizarNomeUsuario( usuario.getNome() );
                finish();
                try {

                    String identificadorUsuario = Base64Custom.codificarBase64( usuario.getEmail() );
                    usuario.setId( identificadorUsuario );
                    usuario.salvar();

                }catch (Exception e){
                    e.printStackTrace();
                }

            }else {

                String excecao = "";
                try {
                    throw task.getException();
                }catch ( FirebaseAuthWeakPasswordException e){
                    excecao = "Digite uma senha mais forte!";
                }catch ( FirebaseAuthInvalidCredentialsException e){
                    excecao = "Por favor, digite um e-mail válido";
                }catch ( FirebaseAuthUserCollisionException e){
                    excecao = "Esta conta já foi cadastrada.";
                }catch ( Exception e ){
                    excecao = "Erro ao cadastrar usuário: " + e.getMessage();
                    e.printStackTrace();
                }

                Toast.makeText(CadastroActivity.this,
                        excecao,
                        Toast.LENGTH_SHORT).show();

            }
        });

    }

    public void validarCadastroUsuario(View view){

        String textoNome  = campoNome.getText().toString();
        String textoEmail = campoEmail.getText().toString();
        String textoSenha = Objects.requireNonNull(campoSenha.getText()).toString();

        if ( !textoNome.isEmpty()){
            if ( !textoEmail.isEmpty()){
                if ( !textoSenha.isEmpty()){

                    Usuario usuario = new Usuario();
                    usuario.setNome( textoNome );
                    usuario.setEmail( textoEmail );
                    usuario.setSenha( textoSenha );

                    cadastrarUsuario( usuario );

                }else {
                    Toast.makeText(CadastroActivity.this, "Preencha a senha!",
                            Toast.LENGTH_SHORT).show();
                }
            }else {
                Toast.makeText(CadastroActivity.this, "Preencha o e-mail!",
                        Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast.makeText(CadastroActivity.this, "Preencha o nome!",
                    Toast.LENGTH_SHORT).show();
        }

    }
}