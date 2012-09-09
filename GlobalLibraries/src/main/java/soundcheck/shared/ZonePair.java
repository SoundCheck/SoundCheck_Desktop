package soundcheck.shared;

import java.io.Serializable;



/**
 * Releates a zoneProp to a peer (keyed to the peer's UID)
 *
 */
public class ZonePair implements Serializable {
	private static final long serialVersionUID = 7659886464086588240L;
	
	private final Zone zone;
	private final ZoneProperties zoneProp;

	public ZonePair(Zone zone, ZoneProperties zoneProp) {
		this.zone = zone;
		this.zoneProp = zoneProp;
	}

	@Override
	public int hashCode() {
		int hashUid = zone != null ? zone.hashCode() : 0;
		int hashZone = zoneProp != null ? zoneProp.hashCode() : 0;

		return (hashUid + hashZone) * hashZone + hashUid;
	}

	/**
	 * Checks if pairs are equal to eachother.
	 * The zoneProp must equal the second pairs zoneProp, and
	 * the zone must equal the second pairs zone. If fields are
	 * null in both pairs, they are also determined to be equivelent.
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof ZonePair) {
			ZonePair otherPair = (ZonePair) other;
			return 
					((  this.zone == otherPair.zone ||
					( this.zone != null && otherPair.zone != null &&
					this.zone.equals(otherPair.zone))) &&
					(      this.zoneProp == otherPair.zoneProp ||
					( this.zoneProp != null && otherPair.zoneProp != null &&
					this.zoneProp.equals(otherPair.zoneProp))) );
		}

		return false;
	}

	@Override
	public String toString()
	{ 
		return "(" + zone + ", " + zoneProp + ")"; 
	}

	public Zone getZone() {
		return zone;
	}

	public ZoneProperties getProp() {
		return zoneProp;
	}
}
