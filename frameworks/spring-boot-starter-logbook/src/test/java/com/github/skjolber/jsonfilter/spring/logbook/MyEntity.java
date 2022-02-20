package com.github.skjolber.jsonfilter.spring.logbook;

public class MyEntity {

    private long id;
    
    private String content;

    private String name;

    public MyEntity() {
    	try {
    		throw new RuntimeException();
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    }

    public MyEntity(long id, String content) {
        this.id = id;
        this.content = content;
    }

    public MyEntity(long id, String content, String name) {
        this.id = id;
        this.content = content;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setContent(String content) {
        this.content = content;
    }
    
    public void setName(String name) {
		this.name = name;
	}
    
    public String getName() {
		return name;
	}
}