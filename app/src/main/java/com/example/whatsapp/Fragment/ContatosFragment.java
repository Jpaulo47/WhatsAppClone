package com.example.whatsapp.Fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import com.example.whatsapp.R;
import com.example.whatsapp.activity.ChatActivity;
import com.example.whatsapp.activity.GrupoActivity;
import com.example.whatsapp.adapter.ContatosAdapter;
import com.example.whatsapp.config.ConfiguracaoFirebase;
import com.example.whatsapp.helper.RecyclerItemClickListener;
import com.example.whatsapp.helper.UsuarioFirebase;
import com.example.whatsapp.model.Usuario;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ContatosFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ContatosFragment extends Fragment {

    private ContatosAdapter adapter;
    private final ArrayList<Usuario> listaContatos = new ArrayList<>();
    private DatabaseReference usuarioRef;
    private ValueEventListener valueEventListenerContatos;
    private FirebaseUser usuariAtual;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ContatosFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ContatosFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ContatosFragment newInstance(String param1, String param2) {
        ContatosFragment fragment = new ContatosFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_contatos, container, false);

        //Configuraçoes iniciais
        RecyclerView recyclerViewListaContatos = view.findViewById(R.id.recyclerViewListaContatos);
        usuarioRef = ConfiguracaoFirebase.getFirebaseDatabase().child("usuarios");
        usuariAtual = UsuarioFirebase.getUsuarioAtual();

        //configurar adapter

        adapter = new ContatosAdapter(listaContatos, getActivity());

        //configurar recyclerView

        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getActivity());
        recyclerViewListaContatos.setLayoutManager( layoutManager );
        recyclerViewListaContatos.setHasFixedSize( true );
        recyclerViewListaContatos.setAdapter( adapter );

        //Configurar evento de click no recyclerView
        recyclerViewListaContatos.addOnItemTouchListener(
                new RecyclerItemClickListener(getActivity(), recyclerViewListaContatos, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {

                        Usuario usuarioSelecionado = listaContatos.get( position );
                        boolean cabecalho = usuarioSelecionado.getEmail().isEmpty();

                        if (cabecalho){

                            Intent i = new Intent( getActivity(), GrupoActivity.class);
                            startActivity( i );

                        }else {
                            Intent i = new Intent(getActivity(), ChatActivity.class);
                            i.putExtra("chatContato", usuarioSelecionado);
                            startActivity( i );

                        }
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {

                    }

                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    }
                }));

        adicionaMenuNovoGrupo();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        recuperarContatos();
    }

    @Override
    public void onStop() {
        super.onStop();
        usuarioRef.removeEventListener( valueEventListenerContatos );
    }

    public void recuperarContatos(){

        valueEventListenerContatos = usuarioRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                limparListaContatos();

                for ( DataSnapshot dados : snapshot.getChildren() ){

                    Usuario usuario = dados.getValue( Usuario.class );
                    String emailUsuarioAtual = usuariAtual.getEmail();

                    assert emailUsuarioAtual != null;
                    assert usuario != null;
                    if ( !emailUsuarioAtual.equals( usuario.getEmail() )){
                        listaContatos.add( usuario );
                    }
                }

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }
    public void limparListaContatos(){

        listaContatos.clear();
        adicionaMenuNovoGrupo();

    }

    public void adicionaMenuNovoGrupo(){

        /* Define usuario com e-mail vazio
         *em caso de e-mail vazio o usuario será ultilizado como
         * cabeçalho, exibindo novo grupo
         * */
        Usuario itemGrupo = new Usuario();
        itemGrupo.setNome("Novo grupo");
        itemGrupo.setEmail("");

        listaContatos.add( itemGrupo);
    }
}