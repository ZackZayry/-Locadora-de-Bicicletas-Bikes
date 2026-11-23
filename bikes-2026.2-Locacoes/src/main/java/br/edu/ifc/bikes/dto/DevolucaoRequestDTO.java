package br.edu.ifc.bikes.dto;

// Corpo opcional da devolução. problema=true envia a bicicleta para MANUTENCAO.
public record DevolucaoRequestDTO(Boolean problema) { }
