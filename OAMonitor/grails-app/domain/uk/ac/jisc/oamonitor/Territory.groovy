package uk.ac.jisc.oamonitor

class Territory extends KBComponent {
  
  static manyByCombo = [
	licenses 	: License,
	packages  	: Package,
	platforms  	: Platform,
	offices  	: Office,
	users		: User,
  ]
  
  static mappedByCombo = [
	licenses 	: 'territories',
	packages  	: 'territories',
	platforms  	: 'territories',
	offices  	: 'territories',
	users		: 'territories',
  ]
  
}
