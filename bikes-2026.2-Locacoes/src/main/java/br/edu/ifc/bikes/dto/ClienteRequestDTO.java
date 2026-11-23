package br.edu.ifc.bikes.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// Cadastro (POST) e atualização (PUT) de cliente.
// O cliente é vinculado ao usuário autenticado; se o email não for informado, usa o username do usuário.
public record ClienteRequestDTO(
        @NotBlank(message = "O nome completo é obrigatório")
        @Size(min = 3, max = 150, message = "O nome deve ter entre 3 e 150 caracteres")
        String nome,

        @NotBlank(message = "O CPF é obrigatório")
        @Pattern(regexp = "\\d{11}", message = "O CPF deve conter 11 dígitos numéricos")
        String cpf,

        @Pattern(regexp = "\\d{10,11}", message = "O telefone deve conter 10 ou 11 dígitos (DDD + número)")
        String telefone,

        @Email(message = "O email deve ser válido")
        @Size(max = 150, message = "O email deve ter no máximo 150 caracteres")
        String email
) { }
