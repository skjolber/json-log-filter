package com.github.skjolber.jsonfilter.spring.logbook;

public class MyOtherEntity {

    private long id1;
    private String content1;
    private String name1;

    public MyOtherEntity() {
    }

    public MyOtherEntity(long id, String content) {
        this.id1 = id;
        this.content1 = content;
    }

    public MyOtherEntity(long id, String content, String name) {
        this.id1 = id;
        this.content1 = content;
        this.name1 = name;
    }

	public long getId1() {
		return id1;
	}

	public void setId1(long id1) {
		this.id1 = id1;
	}

	public String getContent1() {
		return content1;
	}

	public void setContent1(String content1) {
		this.content1 = content1;
	}

	public String getName1() {
		return name1;
	}

	public void setName1(String name1) {
		this.name1 = name1;
	}

    
}