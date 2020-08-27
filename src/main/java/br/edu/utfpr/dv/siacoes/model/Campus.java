package br.edu.utfpr.dv.siacoes.model;

import java.io.Serializable;
import lombok.Data;

public @Data class Campus implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private int idCampus = 0;
	private String name = "";
	private String address = "";
	private transient byte[] logo =  null;
	private boolean active = true;
	private String site = "";
	private String initials = "";
}
