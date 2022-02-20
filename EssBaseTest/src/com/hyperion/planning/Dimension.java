package com.hyperion.planning;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.essbase.api.base.EssException;
import com.essbase.api.metadata.IEssDimension;

public class Dimension {
	private IEssDimension dimension = null;
	private HashMap<String, Member> members = new LinkedHashMap<String, Member>();
	
	public IEssDimension getDimension() {
		return dimension;
	}
	public HashMap<String, Member> getMembers() {
		return members;
	}
	public void setDimension(IEssDimension dimension) {
		this.dimension = dimension;
	}
	public void setMembers(HashMap<String, Member> members) {
		this.members = members;
	}
	public void addMember(Member member) throws EssException{
		members.put(member.getMember().getName(), member);
	}
}