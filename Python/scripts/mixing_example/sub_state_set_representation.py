#!/usr/bin/env python

"""Sub-state set representation of mixing problem with building by function set model
onenote:https://d.docs.live.net/8e55450f976ac566/Notes/AI/Статьи.one#SWRS%20v0.2.5%20Basic%20modeling&
    section-id={40964F6B-F1E6-40E5-93D6-D7D464B4D57F}&page-id={AFB5685E-E61D-4707-B829-BABB190BC41D}&
    object-id={489EDFFB-40E0-073C-3F49-7F005EDECE76}&64
Created 27.04.2018 author CAB
"""

import math as m
import numpy as np
import matplotlib.pyplot as plt


# Script init
print(""" 
#### Sub-state set representation ####
X = [t]
Y = [ω_1, ω_2̂]
G = [v_1, v_2, q_1, q_2, q_3, q_4, ω_3]
""")

# Definitions
class 𝔛_:
    def __init__(self, t):
        self.t = t
    def __repr__(self):
        return f"𝔛 = [t = {self.t}]"
class 𝔜_:
    def __init__(self, ω_1, ω_2):
        self.ω_1 = ω_1
        self.ω_2 = ω_2
    def __repr__(self):
        return f"𝔜 = [ω_1 = {self.ω_1}, ω_2 = {self.ω_2}]"
class 𝔖𝔛_q_:  # Sub-state, 𝔖 ⊆ 𝔜 is sub-stat value, 𝔛 ∈ 𝕏 is key values set, q ∈ ℕ is sub-sate index
    def __init__(self, 𝔛, 𝔜, q):
        self.t = 𝔛.t
        if q == 1:
            self.ω_1 = 𝔜.ω_1
        elif q == 2:
            self.ω_2 = 𝔜.ω_2
        else:
            assert False, "index j can be only  1 or 2"
        self.q = q
    def __repr__(self):
        if self.q == 1:
            return f"𝔖^𝔛_q = 𝔖=[ω_1={self.ω_1}]^𝔛=[t={self.t}]_q=1"
        else:
            return f"𝔖^𝔛_q = 𝔖=[ω_2={self.ω_2}]^𝔛=[t={self.t}]_q=2"

# Function set model
def F(X):
    q = m.sqrt(105.0)
    em = m.exp(((q - 15.0) * X.t) / 16.0)
    ep = m.exp(-(((q + 15.0) * X.t) / 16.0))
    return 𝔜_(
        ω_1 = ((13.0 * em * q) / 21.0) - ((13.0 * ep * q) / 21.0) - (5.0 * em) - (5.0 * ep) + 10.0,
        ω_2 = -((5.0 * em * q) / 21.0) + ((5.0 * ep * q) / 21.0) + (5.0 * em) + (5.0 * ep) + 10.0)

# Generating of set of sub-states
set𝔖X𝔈 = []
for 𝔛 in [𝔛_(t) for t in np.round(np.arange(-15.0, 15.0, 0.1), 4)]:
    𝔜 = F(𝔛)
    set𝔖X𝔈.append(𝔖𝔛_q_(𝔛, 𝔜, q=1))
    set𝔖X𝔈.append(𝔖𝔛_q_(𝔛, 𝔜, q=2))
print("Generated set 𝔖^X|𝔈:")
for 𝔖𝔛_q in set𝔖X𝔈: print("    "+ str(𝔖𝔛_q))


# Simulation function
def simulation(set𝔛):
    set𝔜 = []
    for 𝔛 in set𝔛:
        𝔖𝔛_1 = None
        𝔖𝔛_2 = None
        for 𝔖𝔛_q in set𝔖X𝔈:
            if 𝔖𝔛_q.t == 𝔛.t and 𝔖𝔛_q.q == 1: 𝔖𝔛_1 = 𝔖𝔛_q
            if 𝔖𝔛_q.t == 𝔛.t and 𝔖𝔛_q.q == 2: 𝔖𝔛_2 = 𝔖𝔛_q
        set𝔜.append(𝔜_(𝔖𝔛_1.ω_1, 𝔖𝔛_2.ω_2))
    return set𝔜

# Run simulation
set𝔛 = np.vectorize(lambda t: 𝔛_(t))(np.round(np.arange(0.0, 10.1, 0.1), 4))
set𝔜 = simulation(set𝔛)

# Print result
print("Simulation result (𝔛 -> 𝔜): ")
for [𝔛, 𝔜] in np.column_stack((set𝔛, set𝔜)):
    print(f"    {str(𝔛):30} --> {𝔜}")

# Plot result
plt.figure("Simulation of function set representation")
plt.grid(color="gray")
plt.plot([𝔛.t for 𝔛 in set𝔛], [𝔜.ω_1 for 𝔜 in set𝔜], "g", label="ω_1")
plt.plot([𝔛.t for 𝔛 in set𝔛], [𝔜.ω_2 for 𝔜 in set𝔜], "r", label="ω_2")
plt.show()
