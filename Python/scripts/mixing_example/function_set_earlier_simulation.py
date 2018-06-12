#!/usr/bin/env python

# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#                   Simulation with reactive streams                    #
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

"""Function set representation of mixing problem with simulation by Earlier model
onenote:https://d.docs.live.net/8e55450f976ac566/Notes/AI/Статьи.one#SWRS%20v0.2.5%20Basic%20modeling&
    section-id={40964F6B-F1E6-40E5-93D6-D7D464B4D57F}&page-id={AFB5685E-E61D-4707-B829-BABB190BC41D}&
    object-id={489EDFFB-40E0-073C-3F49-7F005EDECE76}&64
Created 27.04.2018 author CAB
"""

import numpy as np
import matplotlib.pyplot as plt


# Script init
print(""" 
#### Earlier function set representation and simulation ####
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
class 𝔈_:
    def __init__(self, v_1, v_2, q_1, q_2, q_3, q_4, ω_3):
        self.v_1 = v_1
        self.v_2 = v_2
        self.q_1 = q_1
        self.q_2 = q_2
        self.q_3 = q_3
        self.q_4 = q_4
        self.ω_3 = ω_3
    def __repr__(self):
        return f"𝔈 = [v_1 = {self.v_1}, v_2 = {self.v_2}, q_1 = {self.q_1}, q_2 = {self.q_2}, " \
             + f"q_3 = {self.q_3}, q_4 = {self.q_4}, ω_3 = {self.ω_3}]"

# Parameters
𝔈 = 𝔈_(
     v_1 = 4,  # L
     v_2 = 8,  # L
     q_1 = 3,  # L/m
     q_2 = 2,  # L/m
     q_3 = 5,  # L/m
     q_4 = 3,  # L/m
     ω_3 = 10) # g/l
Δt = .1
𝔛_0 = 𝔛_(t=.0)
𝔜_0 = 𝔜_(ω_1=.0, ω_2=20.0)

# Model
def F(X, G, Δt, 𝔛_0, 𝔜_0):
    t = 𝔛_0.t
    ω_1 = 𝔜_0.ω_1
    ω_2 = 𝔜_0.ω_2
    while t <= X.t:
        ω_1_m1 = ω_1
        ω_2_m1 = ω_2
        ω_1 = ω_1_m1 + Δt * (((G.q_1 * G.ω_3) + (G.q_2 * ω_2_m1) - (G.q_3 * ω_1_m1)) / G.v_1)
        ω_2 = ω_2 + Δt * (((G.q_3 * ω_1_m1) - (G.q_2 * ω_2_m1) - (G.q_4 * ω_2_m1)) / G.v_2)
        t += Δt
    return 𝔜_(ω_1, ω_2)

# Simulations
def simulation(setX, G):
    setY = np.array([])
    for X in setX:
        Y = F(X, 𝔈, Δt, 𝔛_0, 𝔜_0)
        setY = np.append(setY, [Y])
    return setY

# Run simulation
setX = np.vectorize(lambda t: 𝔛_(t))(np.arange(0.0, 10.1, 0.1))
setY = simulation(setX, 𝔈)

# Print result
print("Simulation result (𝔛 -> 𝔜): ")
for [𝔛, 𝔜] in np.column_stack((setX, setY)):
    print(f"    {str(𝔛):30} --> {𝔜}")

# Plot result
plt.figure("Simulation of function set representation")
plt.grid(color="gray")
plt.plot([𝔛.t for 𝔛 in setX], [𝔜.ω_1 for 𝔜 in setY], "g", label="ω_1")
plt.plot([𝔛.t for 𝔛 in setX], [𝔜.ω_2 for 𝔜 in setY], "r", label="ω_2")
plt.show()
