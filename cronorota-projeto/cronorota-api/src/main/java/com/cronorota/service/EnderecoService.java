package com.cronorota.service;

import com.cronorota.model.Endereco;
import com.cronorota.repository.EnderecoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

// Implementa o UC04 (Manter pontos e endereços), na parte de endereço.
// RN11 (endereço completo + coordenadas válidas) - a geocodificação real
// (chamar o Nominatim pra obter lat/long a partir do texto do endereço)
// não está implementada aqui ainda; por ora aceitamos o endereço sem
// coordenada, o que é suficiente pra testar o fluxo de tempo parado, mas
// é a peça que falta pra RN11 valer de verdade.
@Service
@RequiredArgsConstructor
public class EnderecoService {

    private final EnderecoRepository enderecoRepository;

    public Endereco cadastrar(String logradouro, String bairro, String cidade, String uf, String cep) {
        Endereco endereco = Endereco.builder()
                .logradouro(logradouro).bairro(bairro).cidade(cidade).uf(uf).cep(cep)
                .build();
        return enderecoRepository.save(endereco);
    }

    public List<Endereco> listarTodos() {
        return enderecoRepository.findAll();
    }
}
