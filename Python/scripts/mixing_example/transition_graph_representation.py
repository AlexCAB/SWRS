#!/usr/bin/env python

# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #
#                   Simulation with reactive streams                    #
# # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # # #

"""Transition graph representation of mixing problem
onenote:https://d.docs.live.net/8e55450f976ac566/Notes/AI/Статьи.one#SWRS%20v0.2.3%20Graph%20modeling&
    section-id={40964F6B-F1E6-40E5-93D6-D7D464B4D57F}&page-id={BB7332E8-6019-4F25-8EB8-970FBE79BC2F}&
    object-id={AB21A1CE-24D1-09BB-1213-C2C15F1AAB66}&22
Created 13.06.2018 author CAB
"""

import numpy as np
import matplotlib.pyplot as plt
import math as m
import networkx as nx
from tools.graph_visualisation import *


# Script init
print(""" 
#### Transition graph representation of mixing problem ####
X = [t]
Y = [ω_1, ω_2̂]
G = [v_1, v_2, q_1, q_2, q_3, q_4, ω_3]
""")

# Value definitions
class b𝔛_:
    def __init__(self, t):
        self.t = t
    def __repr__(self):
        return f"𝔛 = [t={self.t}]"
class h𝔜_:
    def __init__(self, ω_1, ω_2):
        self.ω_1 = ω_1
        self.ω_2 = ω_2
    def __repr__(self):
        return f"𝔜 = [ω_1={self.ω_1}, ω_2={self.ω_2}]"
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
        return f"𝔈 = [v_1={self.v_1}, v_2={self.v_2}, q_1={self.q_1}, q_2={self.q_2}, " \
             + f"q_3={self.q_3}, q_4={self.q_4}, ω_3={self.ω_3}]"
class 𝔓_h_:
    def __init__(self, 𝔈, h):
        if h == 1:
            self.v_1 = 𝔈.v_1
            self.q_1 = 𝔈.q_1
            self.q_2 = 𝔈.q_2
            self.q_3 = 𝔈.q_3
            self.ω_3 = 𝔈.ω_3
        elif h == 2:
            self.v_2 = 𝔈.v_2
            self.q_2 = 𝔈.q_2
            self.q_3 = 𝔈.q_3
            self.q_4 = 𝔈.q_4
        else:
            assert False, "index 'h' can be 1 or 2"
        self.h = h
    def __repr__(self):
        if self.h == 1:
            return f"𝔓_h = [v_1={self.v_1}, q_1={self.q_1}, q_2={self.q_2}, q_3={self.q_3}, ω_3={self.ω_3}]_h=1"
        else:
            return f"𝔓_h = [v_2={self.v_2}, q_2={self.q_2}, q_3={self.q_3}, q_4={self.q_4}]_h=2"
class 𝔖𝔛_q_: # Sub-state, 𝔖 ⊆ 𝔜 is sub-stat value, 𝔛 ∈ 𝕏 is key values set, q ∈ ℕ is sub-sate index
    def __init__(self, t, ω, q):
        self.t = t
        if q == 1:
            self.ω_1 = ω
        elif q == 2:
            self.ω_2 = ω
        else:
            assert False, "index q can be only  1 or 2"
        self.q = q
    def __repr__(self):
        if self.q == 1:
            return f"𝔖^𝔛_q = 𝔖=[ω_1={self.ω_1}]^𝔛=[t={self.t}]_q=1"
        else:
            return f"𝔖^𝔛_q = 𝔖=[ω_2={self.ω_2}]^𝔛=[t={self.t}]_q=2"

# Node definitions
class S_node(NodeLike): # Holds sub-state 𝔖𝔛q, d, w is index of S_node in the Γ graph
    def __init__(self, d, w):
        self.d = d
        self.w = w
        self.__S = None
    def is_defined(self):
        return self.__S is not None
    def assign(self, 𝔖𝔛q):
        assert 𝔖𝔛q is not None
        assert 𝔖𝔛q.q == self.w
        self.__S = 𝔖𝔛q
    def get(self):
        return self.__S
    def graph_repr(self):
        label = f"S_{self.d},{self.w}"
        pos = (self.d, self.w)
        color = "k" if self.is_defined() else "m"
        return label, pos, color, []
    def __repr__(self):
        if self.__S is None:
            return f"S_d,w = (∅)_d={self.d},w={self.w}"
        else:
            return f"S_d,w = ({self.__S})_d={self.d},w={self.d}"
class Θ𝔓_node(NodeLike): # A transaction function
    def __init__(self, S_1, S_2, 𝔓_h, gS, d, w, f_t, f_ω):
        assert S_1.d == (d - 1)
        assert S_2.d == (d - 1)
        assert gS.d == d
        assert S_1.w == 1
        assert S_2.w == 2
        assert gS.w == w
        assert 𝔓_h.h == w
        self.d = d
        self.w = w
        self.__S_1 = S_1
        self.__S_2 = S_2
        self.__𝔓_h  = 𝔓_h
        self.__gS = gS
        self.__f_t = f_t
        self.__f_ω = f_ω
    def eval(self):
        if self.__S_1.is_defined() and self.__S_2.is_defined() and not self.__gS.is_defined():
            assert self.__S_1.get().t == self.__S_2.get().t
            t = self.__f_t(self.__S_1.get().t)
            ω = self.__f_ω(self.__S_1.get().ω_1, self.__S_2.get().ω_2, self.__S_1.get().t, t, self.__𝔓_h)
            self.__gS.assign(𝔖𝔛_q_(t, ω, q=self.w))
            return True
        else:
            return False
    def graph_repr(self):
        label = f"Θ_{self.d},{self.w}"
        pos = (self.d - .3, self.w + (.2 if self.w == 1 else -.2))
        color = "g"
        edges = [(self.__S_1, self), (self.__S_2, self), (self, self.__gS)]
        return label, pos, color, edges
    def __repr__(self):
        return f"{self.__S_1}, {self.__S_2} --> Θ^|𝔓_d={self.d},w={self.w} --> {self.__gS}"

# Graph definitions
class Γ𝔈_graph(GraphLike):
    def __init__(self, setS, setΘ_𝔓, set𝔓):
        for S in setS: assert not S.is_defined()
        self.__setS = setS
        self.__setΘ_𝔓 = setΘ_𝔓
        self.__set𝔓 = set𝔓
    def γ(self, p𝔖):
        for (d, w), 𝔖𝔛_q in p𝔖.items():
            for S in self.__setS:
                if S.d == d and S.w == w: S.assign(𝔖𝔛_q)
        while False in [S.is_defined() for S in self.__setS]:
            for Θ_𝔓 in self.__setΘ_𝔓:
                if Θ_𝔓.eval():
                    self.redraw()
        return γ𝔈_graph(self.__setS, self.__setΘ_𝔓, self.__set𝔓)
    def graph_repr(self):
        return self.__setS + self.__setΘ_𝔓
    def __repr__(self):
        rs = "Γ^|𝔈: \n"
        for Θ_𝔓 in self.__setΘ_𝔓: rs = rs + f"    {str(Θ_𝔓)}\n"
        return rs
class γ𝔈_graph:
    def __init__(self, setS, setΘ_𝔓, set𝔓):
        for S in setS: assert S.is_defined()
        self.__setS = setS
        self.__setΘ_𝔓 = setΘ_𝔓
        self.__set𝔓 = set𝔓
    def 𝔖(self):
        set𝔖X𝔈 = []
        for S in self.__setS:
            set𝔖X𝔈.append(S.get())
        return set𝔖X𝔈
    def __repr__(self):
        rs = "γ^|𝔈: \n"
        for Θ_𝔓 in self.__setΘ_𝔓: rs = rs + f"    {str(Θ_𝔓)}\n"
        return rs

# Γ^|𝔈 builder
def build_Γ𝔈(n, Δt, 𝔈, X_transition, S_transition):
    _𝔓_1 = 𝔓_h_(𝔈, h=1)
    _𝔓_2 = 𝔓_h_(𝔈, h=2)
    f_t = X_transition(Δt)
    f_ω_1, f_ω_2 = S_transition()
    S_1 = S_node(d=0, w=1)
    S_2 = S_node(d=0, w=2)
    setS = [S_1, S_2]
    setΘ_𝔓 = []
    for i in range(1, n):
        gS_1 = S_node(d=i, w=1)
        gS_2 = S_node(d=i, w=2)
        Θ_𝔓_1 = Θ𝔓_node(S_1, S_2, _𝔓_1, gS_1, d=i, w=1, f_t=f_t, f_ω=f_ω_1)
        Θ_𝔓_2 = Θ𝔓_node(S_1, S_2, _𝔓_2, gS_2, d=i, w=2, f_t=f_t, f_ω=f_ω_2)
        setS.extend([gS_1, gS_2])
        setΘ_𝔓.extend([Θ_𝔓_1, Θ_𝔓_2])
        S_1 = gS_1
        S_2 = gS_2
    set𝔓 = [_𝔓_1, _𝔓_2]
    return Γ𝔈_graph(setS, setΘ_𝔓, set𝔓)

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
n = 10
p𝔖 = {
    (0,1): 𝔖𝔛_q_(t=.0, ω=0, q=1),    #State for S_d=0,w=1
    (0,2): 𝔖𝔛_q_(t=.0, ω=20, q=2)}   #State for S_d=0,w=2
use_earlier_transition_function = False

# Transition implementation
def X_transition(Δt):
    def f_t(t):
        return np.round(t + Δt, 4)
    return f_t
def S_functional_transition():
    q = m.sqrt(105.0)
    def em(t): return m.exp(((q - 15.0) * t) / 16.0)
    def ep(t): return m.exp(-(((q + 15.0) * t) / 16.0))
    def f_ω_1(ω_1, ω_2, t_prev, t, 𝔓_1):
        return ((13.0 * em(t) * q) / 21.0) - ((13.0 * ep(t) * q) / 21.0) - (5.0 * em(t)) - (5.0 * ep(t)) + 10.0
    def f_ω_2(ω_1, ω_2, t_prev, t, 𝔓_2):
        return -((5.0 * em(t) * q) / 21.0) + ((5.0 * ep(t) * q) / 21.0) + (5.0 * em(t)) + (5.0 * ep(t)) + 10.0
    return f_ω_1, f_ω_2
def S_earlier_transition():
    def f_ω_1(ω_1, ω_2, t, t_next, 𝔓_1):
        return ω_1 + (t_next - t) * (((𝔓_1.q_1 * 𝔓_1.ω_3) + (𝔓_1.q_2 * ω_2) - (𝔓_1.q_3 * ω_1)) / 𝔓_1.v_1)
    def f_ω_2(ω_1, ω_2, t, t_next, 𝔓_2):
        return ω_2 + (t_next - t) * (((𝔓_2.q_3 * ω_1) - (𝔓_2.q_2 * ω_2) - (𝔓_2.q_4 * ω_2)) / 𝔓_2.v_2)
    return f_ω_1, f_ω_2

# Build Γ^|𝔈
if use_earlier_transition_function:
    Γ𝔈 = build_Γ𝔈(n, Δt, 𝔈, X_transition, S_earlier_transition)
else:
    Γ𝔈 = build_Γ𝔈(n, Δt, 𝔈, X_transition, S_functional_transition)
print(Γ𝔈)
graph_viz = GraphVisualisation("Γ_graph", Γ𝔈, pause=.05)

# Eval γ^|𝔈 for given 𝔖' and get 𝔖^X|𝔈 set
γ𝔈 = Γ𝔈.γ(p𝔖)
print(γ𝔈)
set𝔖X𝔈 = γ𝔈.𝔖()
print("Set 𝔖^X|𝔈 gotten from γ^|𝔈:")
for 𝔖X𝔈 in set𝔖X𝔈: print("    " + str(𝔖X𝔈))

# Simulation function
def simulation(set𝔛):
    set𝔜 = []
    for 𝔛 in set𝔛:
        𝔖𝔛_1 = None
        𝔖𝔛_2 = None
        for 𝔖𝔛_q in set𝔖X𝔈:
            if 𝔖𝔛_q.t == 𝔛.t and 𝔖𝔛_q.q == 1: 𝔖𝔛_1 = 𝔖𝔛_q
            if 𝔖𝔛_q.t == 𝔛.t and 𝔖𝔛_q.q == 2: 𝔖𝔛_2 = 𝔖𝔛_q
        set𝔜.append(h𝔜_(𝔖𝔛_1.ω_1, 𝔖𝔛_2.ω_2))
    return set𝔜

# Run simulation
set𝔛 = np.vectorize(lambda t: b𝔛_(t))(np.round(np.arange(0.0, n * Δt, Δt), 4))
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
