package sistema.rotinas.primefaces.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cad_pso_emb")
public class Tara implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "prd")
    private Long prd; // Código do produto

    @Column(name = "sec")
    private Integer sec;

    @Column(name = "grp")
    private Integer grp;

    @Column(name = "sgr")
    private Integer sgr;

    @Column(name = "pso_emb")
    private Integer psoEmb;

    @Column(name = "dat_atz")
    private LocalDateTime datAtz;

    public Long getPrd() {
        return prd;
    }

    public void setPrd(Long prd) {
        this.prd = prd;
    }

    public Integer getSec() {
        return sec;
    }

    public void setSec(Integer sec) {
        this.sec = sec;
    }

    public Integer getGrp() {
        return grp;
    }

    public void setGrp(Integer grp) {
        this.grp = grp;
    }

    public Integer getSgr() {
        return sgr;
    }

    public void setSgr(Integer sgr) {
        this.sgr = sgr;
    }

    public Integer getPsoEmb() {
        return psoEmb;
    }

    public void setPsoEmb(Integer psoEmb) {
        this.psoEmb = psoEmb;
    }

    public LocalDateTime getDatAtz() {
        return datAtz;
    }

    public void setDatAtz(LocalDateTime datAtz) {
        this.datAtz = datAtz;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prd);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if ((obj == null) || (getClass() != obj.getClass()))
            return false;
        Tara other = (Tara) obj;
        return Objects.equals(prd, other.prd);
    }

    @Override
    public String toString() {
        return "Tara [prd=" + prd + ", sec=" + sec + ", grp=" + grp + ", sgr=" + sgr
                + ", psoEmb=" + psoEmb + ", datAtz=" + datAtz + "]";
    }
}
