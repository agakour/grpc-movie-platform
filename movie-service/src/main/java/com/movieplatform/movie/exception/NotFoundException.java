package com.movieplatform.movie.exception;

public class NotFoundException extends RuntimeException {

	private final String resource;
	private final Object id;

	public NotFoundException(String resource, Object id) {
		super(resource + " " + id + " not found");
		this.resource = resource;
		this.id = id;
	}

	public String getResource() {
		return resource;
	}

	public Object getId() {
		return id;
	}

}
