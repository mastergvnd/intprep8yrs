package com.hyperion.planning;

import java.util.HashMap;
import java.util.LinkedHashMap;

import com.essbase.api.base.EssException;
import com.essbase.api.metadata.IEssMember;

public class Member {
	private IEssMember member = null;
	private HashMap<String, Member> subMembers = new LinkedHashMap<String, Member>();
	
	public IEssMember getMember() {
		return member;
	}
	public HashMap<String, Member> getSubMembers() {
		return subMembers;
	}
	public void setMember(IEssMember member) {
		this.member = member;
	}
	public void setSubMembers(HashMap<String, Member> subMembers) {
		this.subMembers = subMembers;
	}
	public void addSubMember(Member member) throws EssException{
		subMembers.put(member.getMember().getName(), member);
	}
}
