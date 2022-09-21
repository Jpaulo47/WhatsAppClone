package com.example.whatsapp.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.whatsapp.R;
import com.example.whatsapp.adapter.GrupoSelecionadoAdapter;
import com.example.whatsapp.config.ConfiguracaoFirebase;
import com.example.whatsapp.databinding.ActivityCadastroGpBinding;
import com.example.whatsapp.helper.UsuarioFirebase;
import com.example.whatsapp.model.Grupo;
import com.example.whatsapp.model.Usuario;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class CadastroGpActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityCadastroGpBinding binding;
    private final List<Usuario> listaMembrosSelecionados = new ArrayList<>();
    private GrupoSelecionadoAdapter grupoSelecionadoAdapter;
    private static final int SELECAO_GALERIA = 200;
    private StorageReference storageReference;
    private CircleImageView imageGrupo;
    private Grupo grupo;
    private FloatingActionButton fabSalvarGrupo;
    private EditText editNomeGrupo;


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCadastroGpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        setTitle("Novo grupo");
        binding.toolbar.setSubtitle("Defina o nome");

        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        //configurações iniciais
        TextView textTotalParticipantes = findViewById(R.id.textTotalParticipantes);
        RecyclerView recyclerMembrosSelecionados = findViewById(R.id.recyclerMembrosGrupo);
        imageGrupo = findViewById(R.id.imageGrupo);
        fabSalvarGrupo = findViewById(R.id.fabSalvarGrupo);
        editNomeGrupo = findViewById(R.id.editNomeGrupo);
        grupo = new Grupo();
        storageReference = ConfiguracaoFirebase.getFirebaseStorage();

        //Configurar evento de clique
        imageGrupo.setOnClickListener(view -> {
            Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            if (i.resolveActivity(getPackageManager()) != null )
                startActivityForResult(i, SELECAO_GALERIA);
        });

        binding.fabSalvarGrupo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                String nomeGrupo = editNomeGrupo.getText().toString();

                //adiciona usuario logado ao grupo
                listaMembrosSelecionados.add(UsuarioFirebase.getDadosUsuarioLogado());
                grupo.setMembros( listaMembrosSelecionados );

                grupo.setNome( nomeGrupo );
                grupo.salvar();

                Intent i = new Intent(CadastroGpActivity.this, ChatActivity.class);
                i.putExtra("chatGrupo", grupo);
                startActivity( i );
            }
        });

        //recuperar lista de membros passadas
        if ( getIntent().getExtras() != null ){
            List<Usuario> membros = (List<Usuario>) getIntent().getExtras().getSerializable("membros");
            listaMembrosSelecionados.addAll( membros );

            textTotalParticipantes.setText("Participantes: " + listaMembrosSelecionados.size());

        }

        //Configurar recyclerview
        grupoSelecionadoAdapter = new GrupoSelecionadoAdapter(listaMembrosSelecionados, getApplicationContext());
        RecyclerView.LayoutManager layoutManagerHorizontal = new LinearLayoutManager(
                getApplicationContext(),
                LinearLayoutManager.HORIZONTAL,
                false
        );
        recyclerMembrosSelecionados.setLayoutManager( layoutManagerHorizontal );
        recyclerMembrosSelecionados.setHasFixedSize( true );
        recyclerMembrosSelecionados.setAdapter( grupoSelecionadoAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){
            Bitmap imagem = null;
            try{
                Uri localImagemSelecionada = data.getData();
                imagem = MediaStore.Images.Media.getBitmap(getContentResolver(), localImagemSelecionada);

                if (imagem != null){
                    imageGrupo.setImageBitmap(imagem);

                    //recuperar dados da imagem para o firebase
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imagem.compress(Bitmap.CompressFormat.JPEG, 100, baos);
                    byte[] dadosImagem = baos.toByteArray();

                    //salvar imagem no firebase
                    StorageReference imagemRef = storageReference
                            .child("imagens")
                            .child("grupos")
                            .child(grupo.getId() + ".jpeg");

                    //códigos atualizados para o UploadTask das novas bibliotecas
                    UploadTask uploadTask = imagemRef.putBytes(dadosImagem);
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(CadastroGpActivity.this,
                                    "Erro ao fazer upload!", Toast.LENGTH_SHORT).show();
                        }

                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            Toast.makeText(CadastroGpActivity.this,
                                    "Sucesso ao fazer upload!", Toast.LENGTH_SHORT).show();

                            taskSnapshot.getStorage().getDownloadUrl()
                                    .addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {

                                            //converter para string diretamente
                                            grupo.setFoto(uri.toString());
                                        }
                                    });
                        }
                    });
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}