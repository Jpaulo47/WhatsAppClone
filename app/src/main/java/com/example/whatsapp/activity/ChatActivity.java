package com.example.whatsapp.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.whatsapp.R;
import com.example.whatsapp.adapter.MensagensAdapter;
import com.example.whatsapp.config.ConfiguracaoFirebase;
import com.example.whatsapp.databinding.ActivityChatBinding;
import com.example.whatsapp.helper.Base64Custom;
import com.example.whatsapp.helper.UsuarioFirebase;
import com.example.whatsapp.model.Conversa;
import com.example.whatsapp.model.Grupo;
import com.example.whatsapp.model.Mensagem;
import com.example.whatsapp.model.Usuario;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private TextView textViewNome;
    private EditText editMensagem;
    private ImageView imageCamera;
    private CircleImageView circleImageViewFoto;
    private Usuario usuarioDestinatario;
    private DatabaseReference database;
    private DatabaseReference mensagensRef;
    private ChildEventListener childEventListenerMensagens;
    private StorageReference storage;
    private Usuario usuarioRemetente;


    //Identificador usuario remetente e destinatario
    private String idUsuarioRemetente, idUsuarioDestinatario;
    private Grupo grupo;
    private RecyclerView recyclerMensagens;
    private List<Mensagem> mensagens = new ArrayList<>();
    private static final int SELECAO_CAMERA = 100;
    private MensagensAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.example.whatsapp.databinding.ActivityChatBinding binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setTitle("");
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        textViewNome = findViewById(R.id.textViewNomeChat);
        circleImageViewFoto = findViewById(R.id.circleImageFotoChat);
        editMensagem = findViewById(R.id.editMensagem);
        recyclerMensagens = findViewById(R.id.recyclerMensagens);
        imageCamera = findViewById(R.id.imageCamera);

        //recupera dados do usuario remetente
        idUsuarioRemetente = UsuarioFirebase.getIdentificadorUsuario();
        usuarioRemetente = UsuarioFirebase.getDadosUsuarioLogado();

        Bundle bundle = getIntent().getExtras();
        if ( bundle != null ){

            if (bundle.containsKey("chatGrupo")){

                grupo = (Grupo) bundle.getSerializable("chatGrupo");
                idUsuarioDestinatario = grupo.getId();
                textViewNome.setText( grupo.getNome() );

                String foto = grupo.getFoto();
                if ( foto != null ){
                    Uri url = Uri.parse(foto);
                    Glide.with(ChatActivity.this).load( url ).into( circleImageViewFoto );
                }else {
                    circleImageViewFoto.setImageResource(R.drawable.padrao);
                }

            }else{

                usuarioDestinatario = (Usuario) bundle.getSerializable("chatContato");
                textViewNome.setText( usuarioDestinatario.getNome() );

                String foto = usuarioDestinatario.getFoto();
                if ( foto != null ){
                    Uri url = Uri.parse(usuarioDestinatario.getFoto());
                    Glide.with(ChatActivity.this).load( url ).into( circleImageViewFoto );
                }else {
                    circleImageViewFoto.setImageResource(R.drawable.padrao);
                }

                //Recuperar dados do usuario destinatario
                idUsuarioDestinatario= Base64Custom.codificarBase64( usuarioDestinatario.getEmail() );

            }
        }

        //Configurar adapter

        adapter = new MensagensAdapter(mensagens, getApplicationContext());

        //configurar recyclerView
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerMensagens.setLayoutManager( layoutManager );
        recyclerMensagens.setHasFixedSize( true );
        recyclerMensagens.setAdapter( adapter );

        database = ConfiguracaoFirebase.getFirebaseDatabase();
        storage = ConfiguracaoFirebase.getFirebaseStorage();
        mensagensRef = database.child("mensagens")
                .child( idUsuarioRemetente )
                .child( idUsuarioDestinatario );

        //Configurar evento de click na camêra
        imageCamera.setOnClickListener(view -> {
            Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (i.resolveActivity(getPackageManager()) != null )
                startActivityForResult(i, SELECAO_CAMERA);
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK){

            Bitmap imagem = null;

            try{

                switch (requestCode) {

                    case SELECAO_CAMERA:

                        imagem = (Bitmap) data.getExtras().get("data");

                        break;
                }

                if( imagem != null){

                    //Recuperar os dados da imagem para o Firebase
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imagem.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                    byte[] dadosImagem = baos.toByteArray();

                    // Criar nome da imagem / gerar indentificadores unicos em data hora e segundo UUID java.util
                    String nomeImagem = UUID.randomUUID().toString();

                    // Configurar referencia do firebase
                    final StorageReference imagemRef = storage.child("imagens")
                            .child("fotos")
                            .child(idUsuarioRemetente)
                            .child(nomeImagem);

                    UploadTask uploadTask = imagemRef.putBytes(dadosImagem);

                    uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                        @Override
                        public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {

                            if (!task.isSuccessful()) {
                                throw task.getException();
                            }
                            // Continua a tarefa para pegar a URL.
                            return imagemRef.getDownloadUrl();
                        }
                    }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                        @Override
                        public void onComplete(@NonNull Task<Uri> task) {

                            if (task.isSuccessful()) {

                                Uri downloadUri = task.getResult();
                                String downloadUrl = downloadUri.toString();

                                if ( usuarioDestinatario != null ){//mensagem normal

                                    Mensagem mensagem = new Mensagem();
                                    mensagem.setIdUsuario(idUsuarioRemetente);
                                    mensagem.setMensagem("imagem.jpg");
                                    mensagem.setImagem(downloadUrl);

                                    // Salvar mensagem remetente
                                    salvarMensagem(idUsuarioRemetente, idUsuarioDestinatario, mensagem);

                                    // Salvar mensagem destinatario
                                    salvarMensagem(idUsuarioDestinatario, idUsuarioRemetente, mensagem);

                                }else{//mensagem em grupo

                                    for ( Usuario membros: grupo.getMembros()){
                                        String idRemetenteGrupo = Base64Custom.codificarBase64( membros.getEmail());
                                        String idUsuarioLogadoGrupo = UsuarioFirebase.getIdentificadorUsuario();

                                        Mensagem mensagem = new Mensagem();
                                        mensagem.setIdUsuario( idUsuarioLogadoGrupo );
                                        mensagem.setMensagem( "imagem.jpg" );
                                        mensagem.setNome( usuarioRemetente.getNome() );
                                        mensagem.setImagem( downloadUrl );

                                        //salvar mensagem para o membro
                                        salvarMensagem( idRemetenteGrupo, idUsuarioDestinatario, mensagem);

                                        //salvar conversa
                                        salvarConversa(idRemetenteGrupo, idUsuarioDestinatario, usuarioDestinatario, mensagem, true);

                                    }

                                }

                                Toast.makeText(ChatActivity.this, "Foto enviada com sucesso!",
                                        Toast.LENGTH_SHORT).show();

                            } else {
                                Toast.makeText(ChatActivity.this, "Erro em enviar a foto: " + task.getException(),
                                        Toast.LENGTH_SHORT).show();
                            }

                        }
                    });

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void enviarMensagem(View view){

        String textoMensagem = editMensagem.getText().toString();
        if ( !textoMensagem.isEmpty() ){

            if ( usuarioDestinatario != null ){

                Mensagem mensagem = new Mensagem();
                mensagem.setIdUsuario( idUsuarioRemetente );
                mensagem.setMensagem( textoMensagem );

                //Salvar mensagem para o remetente
                salvarMensagem(idUsuarioRemetente, idUsuarioDestinatario, mensagem);

                //Salvar mensagem para o Destinatario
                salvarMensagem(idUsuarioDestinatario, idUsuarioRemetente, mensagem);

                //salvar conversa remetente
                salvarConversa( idUsuarioRemetente, idUsuarioDestinatario, usuarioDestinatario, mensagem, false);

                //salvar conversa destinatario
                salvarConversa(idUsuarioDestinatario, idUsuarioRemetente, usuarioRemetente, mensagem, false);


            }else{

                for ( Usuario membros: grupo.getMembros()){
                    String idRemetenteGrupo = Base64Custom.codificarBase64( membros.getEmail());
                    String idUsuarioLogadoGrupo = UsuarioFirebase.getIdentificadorUsuario();

                    Mensagem mensagem = new Mensagem();
                    mensagem.setIdUsuario( idUsuarioLogadoGrupo );
                    mensagem.setMensagem( textoMensagem );
                    mensagem.setNome( usuarioRemetente.getNome() );

                    //salvar mensagem para o membro
                    salvarMensagem( idRemetenteGrupo, idUsuarioDestinatario, mensagem);

                    //salvar conversa
                    salvarConversa(idRemetenteGrupo, idUsuarioDestinatario, usuarioDestinatario, mensagem, true);

                }
            }
        }else{
            Toast.makeText(ChatActivity.this, "Digite uma mensagem para enviar !",
                    Toast.LENGTH_LONG).show();
        }
    }

    public void salvarConversa(String idRemetente, String idDestinatario, Usuario usuarioExibicao, Mensagem msg, boolean isGroup ){

        //salvar conversa remetente
        Conversa conversaRemetente = new Conversa();
        conversaRemetente.setIdRemetente( idRemetente );
        conversaRemetente.setIdDestinatario( idDestinatario);
        conversaRemetente.setUltimaMensagem( msg.getMensagem() );
        
        if ( isGroup ){//conversa grupo
            conversaRemetente.setIsGroup("true");
            conversaRemetente.setGrupo( grupo );
        }else{//salvar conversa normal
            conversaRemetente.setUsuarioExibicao( usuarioExibicao );
            conversaRemetente.setIsGroup("false");
        }

        conversaRemetente.salvar();
    }

    private void salvarMensagem(String idRemetente, String idDestinatario, Mensagem msg){

        DatabaseReference database = ConfiguracaoFirebase.getFirebaseDatabase();
        DatabaseReference mensagemRef = database.child("mensagens");

        mensagemRef.child(idRemetente)
                .child(idDestinatario)
                .push()
                .setValue( msg );

        //limpar texto
        editMensagem.setText("");
    }

    @Override
    protected void onStart() {
        super.onStart();
        recuperarMensagens();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mensagensRef.removeEventListener( childEventListenerMensagens );
    }

    private void recuperarMensagens(){

        mensagens.clear();
        childEventListenerMensagens = mensagensRef.addChildEventListener(new ChildEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Mensagem mensagem = snapshot.getValue( Mensagem.class);
                mensagens.add( mensagem );
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

}