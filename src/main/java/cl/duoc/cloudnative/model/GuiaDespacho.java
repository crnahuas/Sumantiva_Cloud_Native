package cl.duoc.cloudnative.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "guias_despacho")
public class GuiaDespacho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idGuia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transportista_id")
    private Transportista transportista;

    @Column(nullable = false, length = 6)
    private String fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoGuia estado;

    @Column(nullable = false)
    private String efsPath;

    private String s3Key;

    private long tamanioBytes;

    @Column(nullable = false)
    private Instant timestamp;

    protected GuiaDespacho() {
    }

    public GuiaDespacho(Transportista transportista, String fecha, String efsPath, long tamanioBytes) {
        this.transportista = transportista;
        this.fecha = fecha;
        this.efsPath = efsPath;
        this.tamanioBytes = tamanioBytes;
        this.estado = EstadoGuia.CREADA;
        this.timestamp = Instant.now();
    }

    public Long getIdGuia() {
        return idGuia;
    }

    public Transportista getTransportista() {
        return transportista;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }

    public EstadoGuia getEstado() {
        return estado;
    }

    public void setEstado(EstadoGuia estado) {
        this.estado = estado;
        this.timestamp = Instant.now();
    }

    public String getEfsPath() {
        return efsPath;
    }

    public void setEfsPath(String efsPath) {
        this.efsPath = efsPath;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public long getTamanioBytes() {
        return tamanioBytes;
    }

    public void setTamanioBytes(long tamanioBytes) {
        this.tamanioBytes = tamanioBytes;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
