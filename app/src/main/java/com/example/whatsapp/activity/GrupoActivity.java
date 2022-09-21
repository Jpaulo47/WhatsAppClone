package com.example.whatsapp.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.whatsapp.R;
import com.example.whatsapp.adapter.ContatosAdapter;
import com.example.whatsapp.adapter.GrupoSelecionadoAdapter;
import com.example.whatsapp.config.ConfiguracaoFirebase;
import com.example.whatsapp.databinding.ActivityGrupoBinding;
import com.example.whatsapp.helper.RecyclerItemClickListener;
import com.example.whatsapp.helper.UsuarioFirebase;
import com.example.whatsapp.model.Usuario;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GrupoActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityGrupoBinding binding;
    private ContatosAdapter contatosAdapter;
    private GrupoSelecionadoAdapter grupoSelecionadoAdapter;
    private final List<Usuario> listaMembros = new ArrayList<>();
    private final List<Usuario> listaMembrosSelecionados = new ArrayList<>();
    private ValueEventListener valueEventListenerMembros;
    private DatabaseReference usuarioRef;
    private FirebaseUser usuarioAtual;
    private FloatingActionButton fabAvancarCadastro;

    public void atualizarParticipantesToolbar(){

        int totalSelecionados = listaMembrosSelecionados.size();
        int total = listaMembros.size() + totalSelecionados;

        setSupportActionBar(binding.toolbar);
        binding.toolbar.setSubtitle(totalSelecionados + " de " + total + " selecionados");
    }

   protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityGrupoBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //Configuração toolbar
        setSupportActionBar(binding.toolbar);
        setTitle("Novo Grupo");
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        //Configurações iniciais
       RecyclerView recyclerMembros = findViewById(R.id.recyclerViewMembros);
       RecyclerView recyclerMembrosSelecionados = findViewById(R.id.recyclerMembrosSelecionados);
       fabAvancarCadastro = findViewById(R.id.fabAvancarCadastro);
       usuarioRef = ConfiguracaoFirebase.getFirebaseDatabase().child("usuarios");
       usuarioAtual = UsuarioFirebase.getUsuarioAtual();

       //Confugurações adapter
       contatosAdapter = new ContatosAdapter(listaMembros, getApplicationContext());

       //Configurações recyclerView
       RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
       recyclerMembros.setLayoutManager( layoutManager );
       recyclerMembros.setHasFixedSize(true);
       recyclerMembros.setAdapter( contatosAdapter );

       recyclerMembros.addOnItemTouchListener(new RecyclerItemClickListener(getApplicationContext(),
               recyclerMembros,
               new RecyclerItemClickListener.OnItemClickListener() {
                   @SuppressLint("NotifyDataSetChanged")
                   @Override
                   public void onItemClick(View view, int position) {

                       Usuario usuarioSelecionado = listaMembros.get( position );

                       //Remove usuario selecionado da lista
                       listaMembros.remove( usuarioSelecionado );
                       contatosAdapter.notifyDataSetChanged();

                       //Adicionar usuario na nova lista de selecionados
                       listaMembrosSelecionados.add( usuarioSelecionado );
                       grupoSelecionadoAdapter.notifyDataSetChanged();

                       atualizarParticipantesToolbar();
                   }

                   @Override
                   public void onLongItemClick(View view, int position) {

                   }

                   @Override
                   public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                   }
               }));

       recyclerMembros.addOnItemTouchListener(new RecyclerItemClickListener(
               getApplicationContext(),
               recyclerMembros,
               new RecyclerItemClickListener.OnItemClickListener() {
                   @SuppressLint("NotifyDataSetChanged")
                   @Override
                   public void onItemClick(View view, int position) {

                       Usuario usuarioSelecionado = listaMembros.get(position);

                       //Remove usuario selecionado da lista
                       listaMembros.remove( usuarioSelecionado );
                       contatosAdapter.notifyDataSetChanged();

                       //Adicionar usuario na nova lista de selecionados
                       listaMembrosSelecionados.add( usuarioSelecionado );
                       grupoSelecionadoAdapter.notifyDataSetChanged();

                       atualizarParticipantesToolbar();
                   }

                   @Override
                   public void onLongItemClick(View view, int position) {

                   }

                   @Override
                   public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                   }
               }
       ));

       //Configurar recyclerview para os membros selecionados do grupo
       grupoSelecionadoAdapter = new GrupoSelecionadoAdapter(listaMembrosSelecionados, getApplicationContext());
       RecyclerView.LayoutManager layoutManagerHorizontal = new LinearLayoutManager(
               getApplicationContext(),
               LinearLayoutManager.HORIZONTAL,
               false
       );
       recyclerMembrosSelecionados.setLayoutManager( layoutManagerHorizontal );
       recyclerMembrosSelecionados.setHasFixedSize( true );
       recyclerMembrosSelecionados.setAdapter( grupoSelecionadoAdapter);

       recyclerMembrosSelecionados.addOnItemTouchListener(
               new RecyclerItemClickListener(
                       getApplicationContext(),
                       recyclerMembrosSelecionados,
                       new RecyclerItemClickListener.OnItemClickListener() {
                           @SuppressLint("NotifyDataSetChanged")
                           @Override
                           public void onItemClick(View view, int position) {

                               Usuario usuarioSelecionado = listaMembrosSelecionados.get(position);

                               //Remover da listagem de membrosSelecionados
                               listaMembrosSelecionados.remove( usuarioSelecionado );
                               grupoSelecionadoAdapter.notifyDataSetChanged();

                               //Adicionar listagem de membros
                               listaMembros.add( usuarioSelecionado );
                               contatosAdapter.notifyDataSetChanged();

                               atualizarParticipantesToolbar();
                           }

                           @Override
                           public void onLongItemClick(View view, int position) {

                           }

                           @Override
                           public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                           }
                       }
               )
       );

       fabAvancarCadastro.setOnClickListener(view -> {
           Intent i = new Intent(GrupoActivity.this, CadastroGpActivity.class);
           i.putExtra("membros", (Serializable) listaMembrosSelecionados);
           startActivity( i );
       });

    }

    public void recuperarContatos(){

        valueEventListenerMembros = usuarioRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                for ( DataSnapshot dados : snapshot.getChildren() ){

                    Usuario usuario = dados.getValue( Usuario.class );
                    String emailUsuarioAtual = usuarioAtual.getEmail();

                    assert emailUsuarioAtual != null;
                    assert usuario != null;
                    if ( !emailUsuarioAtual.equals( usuario.getEmail() )){
                        listaMembros.add( usuario );
                    }
                }

                contatosAdapter.notifyDataSetChanged();
                atualizarParticipantesToolbar();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        recuperarContatos();
    }

    @Override
    public void onStop() {
        super.onStop();
        usuarioRef.removeEventListener( valueEventListenerMembros );
    }

}