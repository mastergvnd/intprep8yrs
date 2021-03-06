package paypal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import org.junit.Assert;

public class _B25ReconstructItinerary {

	public static void main(String[] args) {
		List<List<String>> tickets = new ArrayList<List<String>>();
		tickets.add(new ArrayList<String>(Arrays.asList("Chennai", "Banglore")));
		tickets.add(new ArrayList<String>(Arrays.asList("Bombay", "Delhi")));
		tickets.add(new ArrayList<String>(Arrays.asList("Goa", "Chennai")));
		tickets.add(new ArrayList<String>(Arrays.asList("Delhi", "Goa")));
		String source = getItinerarySource(tickets);
		System.out.println("Source : " + source);
		Assert.assertEquals(new ArrayList<String>(Arrays.asList("Bombay", "Delhi", "Goa", "Chennai", "Banglore")), findItinerary(tickets, source));
	
		tickets = new ArrayList<List<String>>();
		tickets.add(new ArrayList<String>(Arrays.asList("JFK_NY", "SFO")));
		tickets.add(new ArrayList<String>(Arrays.asList("JFK_NY", "ATL_GA")));
		tickets.add(new ArrayList<String>(Arrays.asList("SFO", "ATL_GA")));
		tickets.add(new ArrayList<String>(Arrays.asList("ATL_GA", "JFK_NY")));
		tickets.add(new ArrayList<String>(Arrays.asList("ATL_GA", "SFO")));
		Assert.assertEquals(new ArrayList<String>(Arrays.asList("JFK_NY", "ATL_GA", "JFK_NY", "SFO", "ATL_GA", "SFO")), findItinerary(tickets, "JFK_NY"));
		
		tickets = new ArrayList<List<String>>();
		tickets.add(new ArrayList<String>(Arrays.asList("JFK", "KUL")));
		tickets.add(new ArrayList<String>(Arrays.asList("JFK", "NRT")));
		tickets.add(new ArrayList<String>(Arrays.asList("NRT", "JFK")));
		Assert.assertEquals(new ArrayList<String>(Arrays.asList("JFK","NRT","JFK","KUL")), findItinerary(tickets, "JFK"));
	}

	public static List<String> findItinerary(List<List<String>> tickets, String source) {
		List<String> result = new ArrayList<String>();
		if(tickets.size() == 0)
			return result;
		Map<String, PriorityQueue<String>> itinerary = new HashMap<String, PriorityQueue<String>>();
		
		for(List<String> ticket : tickets) {
			PriorityQueue<String> destinations = itinerary.getOrDefault(ticket.get(0), new PriorityQueue<String>());
			destinations.add(ticket.get(1));
			itinerary.put(ticket.get(0), destinations);
		}
		dfs(itinerary, source, result);
		return result;
	}
	
	private static void dfs(Map<String, PriorityQueue<String>> itinerary, String source, List<String> result) {
		PriorityQueue<String> departures = itinerary.get(source);
		while(departures != null && !departures.isEmpty()) {
			dfs(itinerary, departures.poll(), result);
		}
		result.add(0, source);
			
	}

	private static String getItinerarySource(List<List<String>> tickets) {
		Map<String, String> itinerary = new HashMap<>();
		for(List<String> ticket : tickets) {
			itinerary.put(ticket.get(0), ticket.get(1));
		}
		String start = null;
		for(Map.Entry<String, String> entry : itinerary.entrySet()) {
			if(!itinerary.containsValue(entry.getKey()))
				start = entry.getKey();
		}
		return start;
	}

}
