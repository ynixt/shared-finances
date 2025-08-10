local session = std.extVar('session');

{
	claims: {
		identity: { 
			traits: session.identity.traits
		}
	}
}