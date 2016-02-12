#!/usr/bin/python

'''
TOPOLOGY USED IN HOTMIDLLEBOXES 2016 TEST

- 12 switches
- 4 VRs
- 2 VMUs
- 2 final host (Hn)
- 3 WANA (with 2 interfaces each bridged together with a Linux bridge)
- 1 DPI
- 1 TC
all configured as in a L2 scenario
 
1) sudo python hotmboxes16-topo-2016-02-07.py  
'''

from mininet.net import Mininet
from mininet.node import Node
from mininet.node import Host
#from mininet.link import Link
from mininet.link import TCLink
from mininet.link import Intf
from mininet.log import setLogLevel, info
from mininet.cli import CLI
from mininet.node import Controller
from mininet.node import RemoteController
from mininet.util import quietRun

from time import sleep 
import os
import sys

def defineNetwork():
  if len(sys.argv) < 2:
    print "Missing paramenter: python hotmboxes16-topo-2016-02-05.py <debug=1|0>"
    sys.exit()
    #commento
  debug = sys.argv[1] #print some usefull information
  
  net = Mininet(topo=None,
  build=False, link=TCLink)
  net.addController(name='c0',
  controller=RemoteController,
# ip='192.168.224.133',
  port=6633)
  #net = Mininet(controller=RemoteController, link=TCLink, build=False, xterms=False)


  info("*** Create an empty network and add nodes and swith to it *** \n")
  #net = Mininet(controller=RemoteController, link=TCLink, build=False) #MyPOXController
  info("\n*** Adding Controller: Controller will be external *** \n")

  #BUILDING CLUSTER 1: s1, s2, s3, s4 and vmu1, vmu2, dpi, wana, tc
  info("\n*** Creating Switch *** \n")  
  s1 = net.addSwitch('s1') # this should be equivalent to s1 = net.addSwitch('s1', OVSSwitch)
  s1.cmd( 'ovs-vsctl del-br ' + s1.name )
  s1.cmd( 'ovs-vsctl add-br ' + s1.name )
  s1.cmd( 'ovs-vsctl set Bridge '+ s1.name + ' stp_enable=false' ) # Disabling STP
  
  s2 = net.addSwitch('s2')
  s2.cmd( 'ovs-vsctl del-br ' + s2.name )
  s2.cmd( 'ovs-vsctl add-br ' + s2.name )
  s2.cmd( 'ovs-vsctl set Bridge '+ s2.name + ' stp_enable=false' ) # Disabling STP

  s3 = net.addSwitch('s3')
  s3.cmd( 'ovs-vsctl del-br ' + s3.name )
  s3.cmd( 'ovs-vsctl add-br ' + s3.name )
  s3.cmd( 'ovs-vsctl set Bridge '+ s3.name + ' stp_enable=false' ) # Disabling STP

  s4 = net.addSwitch('s4')
  s4.cmd( 'ovs-vsctl del-br ' + s4.name )
  s4.cmd( 'ovs-vsctl add-br ' + s4.name )
  s4.cmd( 'ovs-vsctl set Bridge '+ s4.name + ' stp_enable=false' ) # Disabling STP  

  info("\n*** Creating VM-User 1 *** \n")
  vmu1 = net.addHost('VMU1')
  info("\n*** Creating VM-User 2 *** \n")
  vmu2 = net.addHost('VMU2')
  info("\n*** Creating DPI *** \n")
  dpi = net.addHost('DPI')
  info("\n*** Creating WAN A. *** \n")
  wana = net.addHost('WANA')
  info("\n*** Creating TC *** \n")
  tc = net.addHost('TC')
  info("\n*** Creating Virtual Router 1 *** \n")
  vr1 = net.addHost('VR1')
  info("\n*** Creating Links on Cluster 1 *** \n")
  net.addLink(vmu1, s1, bw=100)
  net.addLink(vmu2, s1, bw=100) 
  net.addLink(dpi, s1, bw=100)
  net.addLink(wana, s1, bw=100)
  net.addLink(wana, s1, bw=100)
  net.addLink(tc, s1, bw=100)
  net.addLink(tc, s1, bw=100)
  net.addLink(s1, s2, bw=100)
  net.addLink(s1, s3, bw=100)
  net.addLink(s2, s4, bw=100)
  net.addLink(s3, s4, bw=100) 
  net.addLink(vr1, s4, bw=100)

  #BUILDING CLUSTER 2: s5, s6, s7, s8 and h2, wana2, vr2, vr3
  s5 = net.addSwitch('s5')
  s5.cmd( 'ovs-vsctl del-br ' + s5.name )
  s5.cmd( 'ovs-vsctl add-br ' + s5.name )
  s5.cmd( 'ovs-vsctl set Bridge '+ s5.name + ' stp_enable=false' ) # Disabling STP

  s6 = net.addSwitch('s6')
  s6.cmd( 'ovs-vsctl del-br ' + s6.name )
  s6.cmd( 'ovs-vsctl add-br ' + s6.name )
  s6.cmd( 'ovs-vsctl set Bridge '+ s6.name + ' stp_enable=false' ) # Disabling STP

  s7 = net.addSwitch('s7')
  s7.cmd( 'ovs-vsctl del-br ' + s7.name )
  s7.cmd( 'ovs-vsctl add-br ' + s7.name )
  s7.cmd( 'ovs-vsctl set Bridge '+ s7.name + ' stp_enable=false' ) # Disabling STP

  s8 = net.addSwitch('s8')
  s8.cmd( 'ovs-vsctl del-br ' + s8.name )
  s8.cmd( 'ovs-vsctl add-br ' + s8.name )
  s8.cmd( 'ovs-vsctl set Bridge '+ s8.name + ' stp_enable=false' ) # Disabling STP

  info("\n*** Creating Host 2 *** \n")
  h2 = net.addHost('H2')
  info("\n*** Creating WAN A. 2 *** \n")
  wana2 = net.addHost('WANA2')
  info("\n*** Creating VR2 *** \n")
  vr2 = net.addHost('VR2')
  info("\n*** Creating VR3 *** \n")
  vr3 = net.addHost('VR3')
  info("\n*** Creating Links on Cluster 2 *** \n")
  net.addLink(vr1, vr2, bw=100)
  net.addLink(vr2, s5, bw=100)
  net.addLink(s5, s6, bw=100)
  net.addLink(s5, s8, bw=100)
  net.addLink(h2, s8, bw=100)
  net.addLink(wana2, s6, bw=100)
  net.addLink(wana2, s6, bw=100)
  net.addLink(s6, s7, bw=100)
  net.addLink(s8, s7, bw=100)
  net.addLink(s7, vr3, bw=100)

  #BUILDING CLUSTER 3: s9, s10, s11, s12 and h3, wana3, vr4 
  s9 = net.addSwitch('s9')
  s9.cmd( 'ovs-vsctl del-br ' + s9.name )
  s9.cmd( 'ovs-vsctl add-br ' + s9.name )
  s9.cmd( 'ovs-vsctl set Bridge '+ s9.name + ' stp_enable=false' ) # Disabling STP

  s10 = net.addSwitch('s10')
  s10.cmd( 'ovs-vsctl del-br ' + s10.name )
  s10.cmd( 'ovs-vsctl add-br ' + s10.name )
  s10.cmd( 'ovs-vsctl set Bridge '+ s10.name + ' stp_enable=false' ) # Disabling STP

  s11 = net.addSwitch('s11')
  s11.cmd( 'ovs-vsctl del-br ' + s11.name )
  s11.cmd( 'ovs-vsctl add-br ' + s11.name )
  s11.cmd( 'ovs-vsctl set Bridge '+ s11.name + ' stp_enable=false' ) # Disabling STP

  s12 = net.addSwitch('s12')
  s12.cmd( 'ovs-vsctl del-br ' + s12.name )
  s12.cmd( 'ovs-vsctl add-br ' + s12.name )
  s12.cmd( 'ovs-vsctl set Bridge '+ s12.name + ' stp_enable=false' ) # Disabling STP

  info("\n*** Creating Host 3 *** \n")
  h3 = net.addHost('H3')
  info("\n*** Creating WAN A. 3 *** \n")
  wana3 = net.addHost('WANA3')
  info("\n*** Creating VR4 *** \n")
  vr4 = net.addHost('VR4')
  info("\n*** Creating Links on Cluster 3 *** \n")
  net.addLink(vr4, vr3, bw=100)
  net.addLink(vr4, s9, bw=100)
  net.addLink(s9, s10, bw=100)
  net.addLink(s9, s12, bw=100)
  net.addLink(wana3, s10, bw=100)
  net.addLink(wana3, s10, bw=100)
  net.addLink(s10, s11, bw=100)
  net.addLink(s11, s12, bw=100)
  net.addLink(s11, h3, bw=100)

  #Trying to assign MAC address to each node of the cluster 1 
  vmu1.setMAC("00:00:00:00:00:01", vmu1.name + "-eth0")
  vmu2.setMAC("00:00:00:00:00:02", vmu2.name + "-eth0")
  dpi.setMAC("00:00:00:00:00:03", dpi.name + "-eth0")
  wana.setMAC("00:00:00:00:00:04", wana.name + "-eth0") 
  wana.setMAC("00:00:00:00:00:05", wana.name + "-eth1")
  tc.setMAC("00:00:00:00:00:06", tc.name + "-eth0")
  tc.setMAC("00:00:00:00:00:07", tc.name + "-eth1")
  vr1.setMAC("00:00:00:00:00:08", vr1.name + "-eth0") 
  vr1.setMAC("00:00:00:00:00:09", vr1.name + "-eth1")
  
  #Trying to assign MAC address to each node of the cluster 2 
  vr2.setMAC("00:00:00:00:00:0A", vr2.name + "-eth0")
  vr2.setMAC("00:00:00:00:00:0B", vr2.name + "-eth1")
  h2.setMAC("00:00:00:00:00:0C", h2.name + "-eth0")
  wana2.setMAC("00:00:00:00:00:0D", wana2.name + "-eth0")
  wana2.setMAC("00:00:00:00:00:0E", wana2.name + "-eth1")
  vr3.setMAC("00:00:00:00:00:0F", vr3.name + "-eth0")
  vr3.setMAC("00:00:00:00:00:10", vr3.name + "-eth1")

  #Trying to assign MAC address to each node of the cluster 3 
  vr4.setMAC("00:00:00:00:00:11", vr4.name + "-eth0")
  vr4.setMAC("00:00:00:00:00:12", vr4.name + "-eth1")
  wana3.setMAC("00:00:00:00:00:13", wana3.name + "-eth0")
  wana3.setMAC("00:00:00:00:00:14", wana3.name + "-eth1")
  h3.setMAC("00:00:00:00:00:15", h3.name + "-eth0")
  
  #Disabling IPv6
  for host in net.hosts:
    print 'Going to disable IPv6 on ' + host.name
    host.cmd('sysctl -w net.ipv6.conf.all.disable_ipv6=1')
    host.cmd('sysctl -w net.ipv6.conf.default.disable_ipv6=1')
    host.cmd('sysctl -w net.ipv6.conf.lo.disable_ipv6=1')

  for switch in net.switches:
    for intf in switch.intfs.values():
      switch.cmd( 'ovs-vsctl add-port ' + switch.name + ' %s' % intf )
      print "Eseguito comando: ovs-vsctl add-port ", switch.name, " ", intf

  #info("\n*** Starting Network using Open vSwitch and remote controller*** \n") 

  # Set the controller for the switch
  for switch in net.switches:
    switch.cmd('ovs-vsctl set-controller ' +  switch.name + ' tcp:127.0.0.1:6633')
    info( '\n*** Waiting for switch to connect to controller' )
    while 'is_connected' not in quietRun( 'ovs-vsctl show' ):
      sleep( 1 )
      info( '.' )
    info('\n')

  # Creating a Linux Bridge on each host
  nhosts = len(net.hosts)
  print 'Total number of hosts: ' + str(nhosts)
  count = 1

  net.start()
  info('\n*** Going to take down default configuration ...\n')
  info('\n*** ... and creating Linux bridge on WANA and TC, as well as configuring interfaces \n')
  for host in net.hosts:
    print 'Deleting ip address on ' + host.name + '-eth0 interface ...'
    host.cmd('ip addr del ' + host.IP(host.name + '-eth0') + '/8 dev ' + host.name + '-eth0')
    print 'Deleting entry in IP routing table on ' + host.name
    host.cmd('ip route del 10.0.0.0/8')
    print "Going to configure new IP" 
    if host.name == 'WANA' or host.name == 'WANA2' or host.name == 'WANA3' or host.name == 'TC': # VNFs case
      print "Host with 2 interfaces: " + host.name
      host.cmd('brctl addbr br-' + host.name)
      host.cmd('brctl addif br-' + host.name + ' ' + host.name + '-eth0')
      host.cmd('brctl addif br-' + host.name + ' ' + host.name + '-eth1')
      if host.name == 'WANA' or host.name == 'TC':
        host.cmd('ip addr add 192.168.1.' + str(count) + '/24 dev br-' + host.name)
      elif host.name == 'WANA2': 
        host.cmd('ip addr add 192.168.2.' + str(count) + '/24 dev br-' + host.name)
      else:
        host.cmd('ip addr add 192.168.3.' + str(count) + '/24 dev br-' + host.name)
      host.cmd('ip link set br-' + host.name + ' up') 
      print "LB configured!"
      host.cmd('sysctl -w net.ipv4.ip_forward=1')
      print "IP Forwarding enabled!"  
    elif host.name == 'VMU1' or host.name == 'VMU2' or host.name == 'DPI' or host.name == 'H2' or host.name == 'H3':
      if host.name == 'VMU1' or host.name == 'VMU2' or host.name == 'DPI': # Machine on cluster 1
        host.setIP("192.168.1." + str(count), 24, host.name + "-eth0")
      elif host.name == 'H2': # Machine on cluster 2
        host.setIP("192.168.2." + str(count), 24, host.name + "-eth0")
      else: # Machine on cluster 3
        host.setIP("192.168.3." + str(count), 24, host.name + "-eth0")
      print "[CURRENT-CHECK] IP: " + net.hosts[count - 1].IP(net.hosts[count - 1].name + '-eth0') 
    elif host.name == 'VR1' or host.name == 'VR3':  
      if host.name == 'VR1':
        host.setIP("192.168.1." + str(count), 24, host.name + "-eth0")
      elif host.name == 'VR3':
        host.setIP("192.168.2." + str(count), 24, host.name + "-eth0")
      net.hosts[count - 1].setIP("10.0.0." + str(count - 5), 30, net.hosts[count - 1].name + "-eth1")
      net.hosts[count + 2].setIP("10.0.0." + str(count - 4), 30, net.hosts[count + 2].name + "-eth0") # also configuring VR2-eth0 and VR4-eth0
      print net.hosts[count - 1].name + "-eth1 interface has been configured!" 
      print "[Checking VR IP] " + net.hosts[count - 1].IP(host.name + '-eth1')
      net.hosts[count - 1].cmd('sysctl -w net.ipv4.ip_forward=1') # enabled on VR1
      print "On VR node: IP Forwarding enabled!" 
    else: # VR2 or VR4 case
      if host.name == 'VR2':
        host.setIP("192.168.2." + str(count), 24, host.name + "-eth1")
      else: # VR4
        host.setIP("192.168.3." + str(count), 24, host.name + "-eth1")
      net.hosts[count - 1].cmd('sysctl -w net.ipv4.ip_forward=1')
      print "On VR node: IP Forwarding enabled!"
    count = count + 1 
    print "\n"

  # ARP storm avoidance rules
#  for switch in net.switches:
#    if switch.name == 's1':
#      switch.cmd('ovs-ofctl add-flow ' + switch.name + ' in_port=4,arp,dl_dst=FF:FF:FF:FF:FF:FF,actions=drop')
#      switch.cmd('ovs-ofctl add-flow ' + switch.name + ' in_port=5,arp,dl_dst=FF:FF:FF:FF:FF:FF,actions=drop')
#      switch.cmd('ovs-ofctl add-flow ' + switch.name + ' in_port=6,arp,dl_dst=FF:FF:FF:FF:FF:FF,actions=drop')
#      switch.cmd('ovs-ofctl add-flow ' + switch.name + ' in_port=7,arp,dl_dst=FF:FF:FF:FF:FF:FF,actions=drop')
#    elif switch.name == 's6' or switch.name == 's10':
#      switch.cmd('ovs-ofctl add-flow ' + switch.name + ' in_port=2,arp,dl_dst=FF:FF:FF:FF:FF:FF,actions=drop')
#      switch.cmd('ovs-ofctl add-flow ' + switch.name + ' in_port=3,arp,dl_dst=FF:FF:FF:FF:FF:FF,actions=drop')

#  for switch in net.switches:
#    print "Rules installed on switch " + switch.name + ": " + switch.cmdPrint('ovs-ofctl dump-flows ' + switch.name)

  print "Configuring default gw on each host.. TODO"
  count = 1 
  for host in net.hosts:
    print "Adding default gw ..."
    '''if host.name != 'VR' and host.name != 'H1' and host.name != 'WANA' and host.name != 'TC': 
      host.setDefaultRoute('dev ' + host.name + '-eth0 via ' + net.hosts[nhosts - 2].IP(net.hosts[nhosts - 2].name + '-eth0'))
    elif host.name == 'TC' or host.name == 'WANA':
      print "Default GW manually configured"
      host.cmd('route add default gw ' + net.hosts[nhosts - 2].IP(net.hosts[nhosts - 2].name + '-eth0'))  
    else:
      #H1 case  
      host.setDefaultRoute('dev ' + host.name + '-eth0 via ' + net.hosts[nhosts - 2].IP(net.hosts[nhosts - 2].name + '-eth1'))'''
    if host.name == 'VMU1' or host.name == 'VMU2':
      host.setDefaultRoute('dev ' + host.name + '-eth0 via ' + net.hosts[nhosts - 8].IP(net.hosts[nhosts - 8].name + '-eth0')) 
    elif host.name == 'H2':
      host.cmd('route add -net 192.168.1.0 netmask 255.255.255.0 gw ' + net.hosts[nhosts - 5].IP(net.hosts[nhosts - 5].name + '-eth1'))
      host.cmd('route add -net 192.168.3.0 netmask 255.255.255.0 gw ' + net.hosts[nhosts - 4].IP(net.hosts[nhosts - 4].name + '-eth0'))
    elif host.name == 'H3':
      host.setDefaultRoute('dev ' + host.name + '-eth0 via ' + net.hosts[nhosts - 1].IP(net.hosts[nhosts - 1].name + '-eth1'))
    elif host.name == 'VR1':
      host.cmd('route add -net 192.168.2.0 netmask 255.255.255.0 gw ' + net.hosts[nhosts - 5].IP(net.hosts[nhosts - 5].name + '-eth0'))
      host.cmd('route add -net 192.168.3.0 netmask 255.255.255.0 gw ' + net.hosts[nhosts - 5].IP(net.hosts[nhosts - 5].name + '-eth0'))
    elif host.name == 'VR2':
      host.cmd('route add -net 192.168.1.0 netmask 255.255.255.0 gw ' + net.hosts[nhosts - 8].IP(net.hosts[nhosts - 8].name + '-eth1'))
      host.cmd('route add -net 192.168.3.0 netmask 255.255.255.0 gw ' + net.hosts[nhosts - 4].IP(net.hosts[nhosts - 4].name + '-eth0'))
    elif host.name == 'VR3':
      host.cmd('route add -net 192.168.1.0 netmask 255.255.255.0 gw ' + net.hosts[nhosts - 5].IP(net.hosts[nhosts - 5].name + '-eth1'))
      host.cmd('route add -net 192.168.3.0 netmask 255.255.255.0 gw ' + net.hosts[nhosts - 1].IP(net.hosts[nhosts - 1].name + '-eth0'))
    elif host.name == 'VR4':
      host.cmd('route add -net 192.168.1.0 netmask 255.255.255.0 gw ' + net.hosts[nhosts - 4].IP(net.hosts[nhosts - 4].name + '-eth1'))
      host.cmd('route add -net 192.168.2.0 netmask 255.255.255.0 gw ' + net.hosts[nhosts - 4].IP(net.hosts[nhosts - 4].name + '-eth1'))
    else:
      print "Host " + host.name + ": routing table currently not configured"

  info('... running CLI \n***')
  CLI(net)
  info('\n')
  info('... stopping Network ***\n')
  net.stop()

#Main
if __name__ == '__main__':
  setLogLevel('info')
  defineNetwork()
