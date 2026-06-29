package cl.duoc.cloudnative.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "transportistas")
public class Transportista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String alias;

    @Column(nullable = false)
    private String credencialDescarga;

    protected Transportista() {
    }

    public Transportista(String alias, String credencialDescarga) {
        this.alias = alias;
        this.credencialDescarga = credencialDescarga;
    }

    public Long getId() {
        return id;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getCredencialDescarga() {
        return credencialDescarga;
    }

    public void setCredencialDescarga(String credencialDescarga) {
        this.credencialDescarga = credencialDescarga;
    }
}
