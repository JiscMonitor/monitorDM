



# Sample ~/.grails/OAMonitor-config.groovy file for dev box

localauth=true
sysusers = [
  [
    name:'admin',
    pass:'localadmpass',
    display:'Admin',
    email:'admin@localhost',
    roles: [ 'ROLE_USER', 'ROLE_ADMIN']
  ]
]
