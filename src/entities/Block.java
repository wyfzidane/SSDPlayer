/*******************************************************************************
 * SSDPlayer Visualization Platform (Version 1.0)
 * Authors: Roman Shor, Gala Yadgar, Eitan Yaakobi, Assaf Schuster
 * Copyright (c) 2015, Technion � Israel Institute of Technology
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that
 * the following conditions are met:
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS 
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. 
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 *******************************************************************************/
package entities;

import general.Consts;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import manager.SSDManager;
import utils.Utils;

public abstract class Block<P extends Page> {
	public static abstract class Builder<P extends Page> {
		private Block<P> block;
		
		public Builder<P> setEraseCounter(int eraseCounter) {
			block.eraseCounter = eraseCounter;
			return this;
		}

		public Builder<P> setStatus(BlockStatus status) {
			block.status = status;
			return this;
		}

		public Builder<P> setInGC(boolean inGC) {
			block.inGC = inGC;
			return this;
		}
		
		public Builder<P> setPagesList(List<P> pagesList) {
			block.displayedPagesList = new ArrayList<P>(pagesList);
			return this;
		}
		
		public Builder<P> setValidCounter(int validCounter) {
			block.validCounter = validCounter;
			return this;
		}
		
		public Builder<P> setManager(SSDManager<P, ?, ?, ?, ?> manager) {
			block.manager = manager;
			return this;
		}
		
		public abstract Block<P> build();
		
		protected void validate() {
			Utils.validateNotNull(block.displayedPagesList, "pagesList");
			Utils.validateNotNull(block.status, "status");
			Utils.validateNotNegative(block.eraseCounter, "erase Counter");
		}

		protected void setBlock(Block<P> block) {
			this.block = block;
		}
	}
	
	private List<P> displayedPagesList;
	private int eraseCounter = -1;
	private int validCounter = 0;
	private BlockStatus status = null; 
	private boolean inGC = false;
	private SSDManager<P, ?, ?, ?, ?> manager = null;

	protected Block() { }
	
	protected Block(Block<P> other) {
		displayedPagesList = new ArrayList<P>(other.displayedPagesList);
		eraseCounter = other.eraseCounter;
		status = other.status;
		inGC = other.inGC;
		validCounter = other.validCounter;
		manager = other.manager;
	}

	abstract public Builder<P> getSelfBuilder();

	public Iterable<P> getDisplayedPages() {
		return displayedPagesList;
	}
	
	public P getPage(int i) {
		return displayedPagesList.get(i);
	}
	
	public int getPagesNum() {
		return displayedPagesList.size();
	}

	public int getEraseCounter() {
		return eraseCounter;
	}

	public int getValidCounter() {
		return validCounter;
	}
	

	public BlockStatus getStatus() {
		return status;
	}

	public boolean isInGC() {
		return inGC;
	}
	
	public List<P> getNewPagesList() {
		return new ArrayList<P>(displayedPagesList);
	}

	public int getCleanPageIndex() {
		for (int i =0; i < displayedPagesList.size()-1; ++i) {
			Page page = displayedPagesList.get(i);
			if (page.isClean()) {
				return i;
			}
		}
		return -1;
	}

	public Color getBGColor() {
		return Consts.Colors.BG;
	}

	public String getStatusName() {
		return status.getDsiplayName();
	}

	public Color getStatusColor() {
		return status.getColor();
	}

	public Color getFrameColor() {
		return null;
	}
	
	public Block<P> invalidate(int lp) {
		boolean wasFound = false;
		List<P> pages = new ArrayList<P>(getPagesNum());
		for (P page : getDisplayedPages()) {
			if (page.isValid() && (page.getLp() == lp)) {
				page = invalidate(page);
				wasFound = true;
			}
			pages.add(page);
		}
		Builder<P> builder = getSelfBuilder();
		builder.setPagesList(pages);
		if (wasFound) {
			builder.setValidCounter(getValidCounter()-1);
		}
		return builder.build();
	}
	
	//Using getSelfBuilder so the build should return the same type - P
	@SuppressWarnings("unchecked")
	public P invalidate(P page) {
		Page.Builder builder = page.getSelfBuilder();
		builder.setValid(false);
		return (P) builder.build();
	}
	
	public Block<P> eraseBlock() {
		Builder<P> builder = getSelfBuilder();
		builder.setPagesList(getEmptyPages())
			.setEraseCounter(getEraseCounter() + 1)
			.setValidCounter(0)
			.setStatus(BlockStatusGeneral.CLEAN);
		return builder.build();
	}
	
	public boolean hasRoomForWrite() {
		for (P page : getDisplayedPages()) {
			if (page.isClean()) {
				return true;
			}
		}
		return false;
	}

	public Block<P> setStatus(BlockStatus status) {
		Builder<P> builder = getSelfBuilder();
		builder.setStatus(status);
		return builder.build();
	}
	
	protected Block<P> addValidPage(int index, P page) {
		List<P> newPagesList = getNewPagesList();
		newPagesList.set(index, page);
		Builder<P> blockBuilder = getSelfBuilder();
		blockBuilder.setPagesList(newPagesList).setValidCounter(getValidCounter()+1);
		return blockBuilder.build();
	}
	
	private List<P> getEmptyPages() {
		List<P> pages = new ArrayList<P>(getPagesNum());
		P page = manager.getEmptyPage();
		for (int i = 0; i < getPagesNum(); i++) {
			pages.add(page);
		}	
		return pages;
	}
}