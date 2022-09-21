package com.example.whatsapp.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.whatsapp.R;
import com.example.whatsapp.config.ConfiguracaoFirebase;
import com.example.whatsapp.helper.Base64Custom;
import com.example.whatsapp.helper.Permissao;
import com.example.whatsapp.helper.UsuarioFirebase;
import com.example.whatsapp.model.Usuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ConfiguracoesActivity extends AppCompatActivity {

    private String[] permissoesNecessarias = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    private static final int SELECAO_CAMERA = 100;
    private static final int SELECAO_GALERIA = 200;
    private CircleImageView circleImageViewPerfil;
    private EditText editPerfilNome;
    private StorageReference storageReference;
    private String identificadorUsuario;
    private Usuario usuarioLogado;

    @SuppressLint("QueryPermissionsNeeded")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes);

        identificadorUsuario = UsuarioFirebase.getIdentificadorUsuario();
        storageReference = ConfiguracaoFirebase.getFirebaseStorage();
        usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();

        //Validar permissões
        Permissao.validarPermissoes(permissoesNecessarias, this, 1);
        ImageButton imageButtonCamera = findViewById(R.id.imageButtonCamera);
        ImageButton imageButtonGaleria = findViewById(R.id.imageButtonGaleria);
        ImageView imageAtualizarNome = findViewById(R.id.imageAtualizarNome);
        circleImageViewPerfil = findViewById(R.id.CircleImagePerfil);
        editPerfilNome = findViewById(R.id.editPerfilNome);

        Toolbar toolbar = findViewById(R.id.toolbarPrincipal);
        toolbar.setTitle("Configurações");
        setSupportActionBar( toolbar );
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        //Recuperar dados du usuario
        FirebaseUser usuario = UsuarioFirebase.getUsuarioAtual();
        Uri url = usuario.getPhotoUrl();

        if ( url != null ){
            Glide.with(ConfiguracoesActivity.this)
                    .load( url )
                    .into( circleImageViewPerfil );
        }else {
            circleImageViewPerfil.setImageResource(R.drawable.padrao);
        }

        editPerfilNome.setText( usuario.getDisplayName() );

        imageButtonCamera.setOnClickListener(view -> {
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (i.resolveActivity(getPackageManager()) != null )
            startActivityForResult(i, SELECAO_CAMERA);
        });

        imageButtonGaleria.setOnClickListener(view -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            if (i.resolveActivity(getPackageManager()) != null )
                startActivityForResult(i, SELECAO_GALERIA);
        });

        imageAtualizarNome.setOnClickListener(view -> {

            String nome = editPerfilNome.getText().toString();
            boolean retorno = UsuarioFirebase.atualizarNomeUsuario( nome );

            if ( retorno ){

                usuarioLogado.setNome( nome );
                usuarioLogado.atualizar();
                Toast.makeText(ConfiguracoesActivity.this, "Nome Alterado com sucesso!",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ( resultCode == RESULT_OK ){
            Bitmap imagem = null;

            try {

                switch (requestCode){
                    case SELECAO_CAMERA:
                        assert data != null;
                        imagem = (Bitmap) data.getExtras().get("data");
                        break;
                    case SELECAO_GALERIA:
                        assert data != null;
                        Uri localImagemSelecionada = data.getData();
                        imagem = MediaStore.Images.Media.getBitmap(getContentResolver(), localImagemSelecionada);
                        break;
                }

                if ( imagem != null ){
                    circleImageViewPerfil.setImageBitmap( imagem );

                    //recupera deados da imagem para o firebase
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imagem.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    byte[] dadosImagem = baos.toByteArray();

                    //salvar imagem no firebase
                    final StorageReference imagemRef = storageReference
                            .child("imagens")
                            .child("perfil")
                            .child(identificadorUsuario)
                            .child("perfil.jpeg");

                    UploadTask uploadTask = imagemRef.putBytes( dadosImagem );
                    uploadTask.addOnFailureListener(e -> {
                        Toast.makeText(ConfiguracoesActivity.this,
                                "Erro ao fazer upload da imagem",
                                Toast.LENGTH_SHORT).show();

                    }).addOnSuccessListener(taskSnapshot -> {
                        Toast.makeText(ConfiguracoesActivity.this,
                                "Sucesso ao fazer upload da imagem",
                                Toast.LENGTH_SHORT).show();

                        imagemRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                atualizarFotoUsuario( uri );
                            }
                        });

                    });

                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void atualizarFotoUsuario(Uri url){
        boolean retorno = UsuarioFirebase.atualizarFotoUsuario( url );
        if ( retorno ){
            usuarioLogado.setFoto( url.toString() );
            usuarioLogado.atualizar();

            Toast.makeText(ConfiguracoesActivity.this,
                    "Sucesso ao alterar sua foto",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for ( int permissaoResultado : grantResults ){
            if ( permissaoResultado == PackageManager.PERMISSION_DENIED){
                alertaValidacaoPermicao();
            }

        }
    }

    private void alertaValidacaoPermicao(){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permissões negadas");
        builder.setMessage("Para utilizar o app é necessario aceitar as permissões");
        builder.setCancelable(false);
        builder.setPositiveButton("Confirmar", (dialogInterface, i) ->
                finish());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}