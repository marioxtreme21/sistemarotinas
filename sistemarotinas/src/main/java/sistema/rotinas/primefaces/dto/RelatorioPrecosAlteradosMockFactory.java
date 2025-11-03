package sistema.rotinas.primefaces.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory usada SOMENTE em tempo de design (Jaspersoft Studio)
 * para popular o Data Adapter "Collection of JavaBeans".
 */
public class RelatorioPrecosAlteradosMockFactory {

    /** Método estático que o Studio vai chamar. */
    public static List<RelatorioPrecosAlteradosDTO> getData() {
        List<RelatorioPrecosAlteradosDTO> list = new ArrayList<>();

        // ===== registro 1 =====
        RelatorioPrecosAlteradosDTO a = new RelatorioPrecosAlteradosDTO();
        a.setLoja(102);
        a.setCodigo(52442L);
        a.setEan("7890000000012");
        a.setDescricao("ABOBORA SECA KG");
        a.setPromocao("N");
        a.setPrecoNormal(4.99);
        a.setPrecoPromocional(4.99);
        a.setDataInicioPromocao(null);
        a.setDataFimPromocao(null);
        a.setDataAlteracao("22/09/2025 00:30:05");
        a.setSeqfamilia(6812L);
        a.setOrdemCategoriaN1("5-HORTI - 6812");
        a.setOrdemCategoriaN2(null);
        a.setCodCatN1(6812);
        a.setCodCatN2(null);
        a.setQtdembalagem(1);
        a.setBreakN1(1);
        a.setBreakN2(1);
        list.add(a);

        // ===== registro 2 (mesma cat N1) =====
        RelatorioPrecosAlteradosDTO b = new RelatorioPrecosAlteradosDTO();
        b.setLoja(102);
        b.setCodigo(12513L);
        b.setEan("7890000000029");
        b.setDescricao("ABOBRINHA VDE KG");
        b.setPromocao("P");
        b.setPrecoNormal(5.99);
        b.setPrecoPromocional(5.29);
        b.setDataInicioPromocao("22/09/2025 00:00:00");
        b.setDataFimPromocao("23/09/2025 23:59:59");
        b.setDataAlteracao("22/09/2025 00:30:05");
        b.setSeqfamilia(6812L);
        b.setOrdemCategoriaN1("5-HORTI - 6812");
        b.setOrdemCategoriaN2(null);
        b.setCodCatN1(6812);
        b.setCodCatN2(null);
        b.setQtdembalagem(1);
        b.setBreakN1(0);
        b.setBreakN2(0);
        list.add(b);

        // ===== registro 3 (nova cat N1 e N2) =====
        RelatorioPrecosAlteradosDTO c = new RelatorioPrecosAlteradosDTO();
        c.setLoja(102);
        c.setCodigo(1963430L);
        c.setEan("7891234567890");
        c.setDescricao("ABS INTIMUS S/V C AB LMPM C32 N1");
        c.setPromocao("N");
        c.setPrecoNormal(25.99);
        c.setPrecoPromocional(25.99);
        c.setDataAlteracao("22/09/2025 00:30:05");
        c.setSeqfamilia(6810L);
        c.setOrdemCategoriaN1("3-LIMPE/USO PESSOAL/INSET - 6810");
        c.setOrdemCategoriaN2(null);
        c.setCodCatN1(6810);
        c.setCodCatN2(null);
        c.setQtdembalagem(1);
        c.setBreakN1(1);
        c.setBreakN2(1);
        list.add(c);

        return list;
    }
}
