package br.edu.ifc.bikes.entity;

public enum StatusBicicleta {
    DISPONIVEL,  // livre para locação
    ALUGADA,     // em uma locação ativa (definido automaticamente)
    MANUTENCAO   // indisponível por problema/manutenção
}
