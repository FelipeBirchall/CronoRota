package com.cronorota.relatorio;

import com.cronorota.dto.response.HistoricoResponse;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;

/**
 * Relatório do período em PDF (OpenPDF), A4 paisagem - a tabela tem oito
 * colunas e o endereço é longo. Usa a identidade visual da seção 1.2 do
 * documento: verde-petróleo nos títulos e no cabeçalho da tabela.
 */
public final class RelatorioPdf {

    private static final Color PETROLEO = new Color(0x1F, 0x6F, 0x78);
    private static final Color GRAFITE = new Color(0x59, 0x59, 0x59);
    private static final Color LINHA = new Color(0xE7, 0xE5, 0xE1);

    private static final Font TITULO = new Font(Font.HELVETICA, 16, Font.BOLD, PETROLEO);
    private static final Font ROTULO = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAFITE);
    private static final Font TEXTO = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.BLACK);
    private static final Font TEXTO_FORTE = new Font(Font.HELVETICA, 9, Font.BOLD, Color.BLACK);
    private static final Font CABECALHO = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font RODAPE = new Font(Font.HELVETICA, 8, Font.NORMAL, GRAFITE);

    private RelatorioPdf() {
    }

    public static byte[] gerar(DadosRelatorio dados) {
        HistoricoResponse h = dados.historico();
        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate(), 36, 36, 36, 42);

        try {
            PdfWriter writer = PdfWriter.getInstance(documento, saida);
            writer.setPageEvent(new Rodape(Formatacao.dataHora(dados.geradoEm())));
            documento.addTitle("CronoRota - Histórico de tempos parados");
            documento.addCreator("CronoRota");
            documento.open();

            documento.add(new Paragraph("Histórico de tempos parados", TITULO));

            // UC14 passo 4: período, filtro e data/hora de geração.
            PdfPTable cabecalho = new PdfPTable(new float[]{1, 4});
            cabecalho.setWidthPercentage(50);
            cabecalho.setHorizontalAlignment(Element.ALIGN_LEFT);
            cabecalho.setSpacingBefore(6);
            infoCabecalho(cabecalho, "Período", Formatacao.periodo(h.inicio(), h.fim()));
            infoCabecalho(cabecalho, "Filtro", dados.filtro());
            infoCabecalho(cabecalho, "Gerado em", Formatacao.dataHora(dados.geradoEm()));
            documento.add(cabecalho);

            // Totalizadores logo no topo: é o que o leitor procura primeiro.
            PdfPTable totais = new PdfPTable(5);
            totais.setWidthPercentage(100);
            totais.setSpacingBefore(12);
            totalizador(totais, "Tempo total parado", Formatacao.minutos(h.tempoTotalParadoMinutos()));
            totalizador(totais, "Média por roteiro", Formatacao.decimal(h.mediaPorRoteiroMinutos()) + " min");
            totalizador(totais, "Média por ponto", Formatacao.decimal(h.mediaPorPontoMinutos()) + " min");
            totalizador(totais, "Roteiros", String.valueOf(h.quantidadeRoteiros()));
            totalizador(totais, "Paradas", String.valueOf(h.quantidadePontos()));
            documento.add(totais);

            if (dados.linhas().isEmpty()) {
                Paragraph vazio = new Paragraph("Nenhum roteiro encontrado para o período informado.", TEXTO);
                vazio.setSpacingBefore(16);
                documento.add(vazio);
            } else {
                documento.add(tabelaDeParadas(dados));
            }
        } catch (DocumentException e) {
            // UC14-E2: o chamador registra em log e devolve o erro à tela.
            throw new IllegalStateException("Falha ao gerar o PDF do relatório", e);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return saida.toByteArray();
    }

    private static PdfPTable tabelaDeParadas(DadosRelatorio dados) {
        PdfPTable tabela = new PdfPTable(new float[]{1.1f, 1.6f, 0.7f, 0.6f, 4.2f, 0.8f, 0.8f, 1.1f});
        tabela.setWidthPercentage(100);
        tabela.setSpacingBefore(14);
        // Repete o cabeçalho da tabela no topo de cada página.
        tabela.setHeaderRows(1);

        for (String titulo : new String[]{"Data", "Motorista", "Roteiro", "Ponto", "Endereço", "Chegada", "Saída", "Tempo parado"}) {
            PdfPCell celula = new PdfPCell(new Phrase(titulo, CABECALHO));
            celula.setBackgroundColor(PETROLEO);
            celula.setBorder(Rectangle.NO_BORDER);
            celula.setPadding(5);
            tabela.addCell(celula);
        }

        for (HistoricoResponse.Linha l : dados.linhas()) {
            celula(tabela, Formatacao.data(l.data()), TEXTO, Element.ALIGN_LEFT);
            celula(tabela, l.motorista(), TEXTO, Element.ALIGN_LEFT);
            celula(tabela, "#" + l.roteiroId(), TEXTO, Element.ALIGN_LEFT);
            celula(tabela, String.valueOf(l.ordem()), TEXTO, Element.ALIGN_LEFT);
            celula(tabela, l.endereco(), TEXTO, Element.ALIGN_LEFT);
            celula(tabela, Formatacao.hora(l.dataHoraChegada()), TEXTO, Element.ALIGN_LEFT);
            celula(tabela, Formatacao.hora(l.dataHoraSaida()), TEXTO, Element.ALIGN_LEFT);
            celula(tabela, Formatacao.minutos(l.tempoParadoMinutos()), TEXTO_FORTE, Element.ALIGN_RIGHT);
        }
        return tabela;
    }

    private static void celula(PdfPTable tabela, String texto, Font fonte, int alinhamento) {
        PdfPCell celula = new PdfPCell(new Phrase(texto, fonte));
        celula.setBorder(Rectangle.BOTTOM);
        celula.setBorderColor(LINHA);
        celula.setPadding(4);
        celula.setHorizontalAlignment(alinhamento);
        tabela.addCell(celula);
    }

    private static void infoCabecalho(PdfPTable tabela, String rotulo, String valor) {
        PdfPCell r = new PdfPCell(new Phrase(rotulo, ROTULO));
        r.setBorder(Rectangle.NO_BORDER);
        r.setPaddingBottom(2);
        tabela.addCell(r);
        PdfPCell v = new PdfPCell(new Phrase(valor, TEXTO));
        v.setBorder(Rectangle.NO_BORDER);
        v.setPaddingBottom(2);
        tabela.addCell(v);
    }

    private static void totalizador(PdfPTable tabela, String rotulo, String valor) {
        Phrase conteudo = new Phrase();
        conteudo.add(new Phrase(rotulo + "\n", ROTULO));
        conteudo.add(new Phrase(valor, new Font(Font.HELVETICA, 13, Font.BOLD, Color.BLACK)));
        PdfPCell celula = new PdfPCell(conteudo);
        celula.setBorderColor(LINHA);
        celula.setPadding(8);
        tabela.addCell(celula);
    }

    // "Página N · gerado em ..." no pé de cada página.
    private static final class Rodape extends PdfPageEventHelper {
        private final String geradoEm;

        Rodape(String geradoEm) {
            this.geradoEm = geradoEm;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document documento) {
            Phrase texto = new Phrase("CronoRota · gerado em " + geradoEm + " · página " + writer.getPageNumber(), RODAPE);
            com.lowagie.text.pdf.ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT, texto,
                    documento.right(), documento.bottom() - 20, 0);
        }
    }
}
