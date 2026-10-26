package com.daoukiwoom.roomflow.Service;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record OffsetLimit(int offset, int limit) implements Pageable {

    @Override
    public int getPageNumber() {
        return limit <= 0 ? 0 : offset / limit;
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    public Sort getSort() {
        return Sort.unsorted();
    }

    @Override
    public Pageable next() {
        return new OffsetLimit(offset + limit, limit);
    }

    @Override
    public Pageable previousOrFirst() {
        return new OffsetLimit(Math.max(0, offset - limit), limit);
    }

    @Override
    public Pageable first() {
        return new OffsetLimit(0, limit);
    }

    @Override
    public Pageable withPage(int pageNumber) {
        return new OffsetLimit(pageNumber * limit, limit);
    }

    @Override
    public boolean hasPrevious() {
        return offset > 0;
    }
}
