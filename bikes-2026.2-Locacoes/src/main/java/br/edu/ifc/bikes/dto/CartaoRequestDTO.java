package br.edu.ifc.bikes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.LuhnCheck;

// Dados do cartão: só trafegam na requisição, não são salvos (exceto titular e final).
public record CartaoRequestDTO(
        @NotBlank(message = "O nome do titular é obrigatório")
        @Size(max = 100, message = "O nome do titular deve ter no máximo 100 caracteres")
        String titular,

        @NotBlank(message = "O número do cartão é obrigatório")
        @Pattern(regexp = "\\d{13,19}", message = "O número do cartão deve conter de 13 a 19 dígitos")
        @LuhnCheck(message = "Número de cartão inválido")
        String numero,

        @NotBlank(message = "A validade é obrigatória")
        @Pattern(regexp = "(0[1-9]|1[0-2])/\\d{2}", message = "A validade deve estar no formato MM/AA")
        String validade,

        @NotBlank(message = "O CVV é obrigatório")
        @Pattern(regexp = "\\d{3,4}", message = "O CVV deve conter 3 ou 4 dígitos")
        String cvv
) {
    // evita vazar número/CVV em logs caso o record seja impresso
    @Override
    public String toString() {
        return "CartaoRequestDTO[titular=" + titular + ", numero=****, validade=**/**, cvv=***]";
    }
}
