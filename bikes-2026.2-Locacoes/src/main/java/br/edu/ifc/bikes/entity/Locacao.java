package br.edu.ifc.bikes.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "locacoes")
public class Locacao extends Auditoria implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bicicleta_id", nullable = false)
    private Bicicleta bicicleta;

    @Column(name = "data_hora_retirada", nullable = false)
    private LocalDateTime dataHoraRetirada;

    @Column(name = "data_hora_prevista_devolucao", nullable = false)
    private LocalDateTime dataHoraPrevistaDevolucao;

    // nulo enquanto a locação está ativa
    @Column(name = "data_hora_devolucao_efetiva")
    private LocalDateTime dataHoraDevolucaoEfetiva;

    // Cartão: por segurança só guardamos o titular e os 4 últimos dígitos.
    // O número completo e o CVV são usados apenas na requisição e nunca persistidos.
    @Column(name = "cartao_titular", nullable = false, length = 100)
    private String cartaoTitular;

    @Column(name = "cartao_final", nullable = false, length = 4)
    private String cartaoFinal;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusLocacao status = StatusLocacao.ATIVA;

    // preenchidos na devolução
    @Column(name = "minutos_utilizados")
    private Long minutosUtilizados;

    @Column(name = "valor_total", precision = 10, scale = 2)
    private BigDecimal valorTotal;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Locacao other)) return false;
        return id != null && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
