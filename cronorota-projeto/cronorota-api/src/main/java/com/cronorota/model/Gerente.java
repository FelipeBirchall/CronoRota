package com.cronorota.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.envers.Audited;

// Sem atributos próprios além dos herdados de Usuario, no MVP. O "dono da
// transportadora" (seção 5 do documento) usa este mesmo perfil, conforme o
// próprio documento definiu: "No MVP utiliza o perfil de gerente/coordenador."
@Entity
@Audited
@Table(name = "gerente")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Gerente extends Usuario {
}
