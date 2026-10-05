package br.com.agendafacil.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Datas por extenso em português ("Segunda-feira, 12 de outubro") para as telas de agendamento. */
final class DatasFormatadas {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter POR_EXTENSO = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR);

    private DatasFormatadas() {
    }

    static String porExtenso(LocalDate data) {
        String texto = POR_EXTENSO.format(data);
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
